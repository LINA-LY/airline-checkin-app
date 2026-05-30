const admin = require("firebase-admin");

const AIRPORTS = require("./data/airports.json");

const PROJECT_ID = process.env.FIREBASE_PROJECT_ID || undefined;

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId: PROJECT_ID
});

const db = admin.firestore();

const AIRCRAFT_TEMPLATES = require("./data/aircrafts.json");

const REGION_DATA = require("./data/regions.json");
const REGION_CODES = REGION_DATA.regionCodes;
// keys in countryRegionMap are lowercase. We'll normalize incoming country names.
const COUNTRY_REGION_MAP = Object.assign({}, REGION_DATA.countryRegionMap);

const FLIGHT_PREFIXES = ["AH", "AF", "BA", "DL", "UA", "AA", "EK", "QR", "LH", "AZ"];
const AIRLINES = [
  "Airline Asia",
  "SkyConnect",
  "BlueWings",
  "Atlas Air",
  "NileJet",
  "Nordic Air",
  "Sahara Air",
  "Aurora Airlines",
  "Coastal Express",
  "Mountain Air"
];

const MIN_FLIGHTS = envInt("MIN_FLIGHTS", 6);
const MAX_FLIGHTS = envInt("MAX_FLIGHTS", 12);
const MIN_DAYS_AHEAD = envInt("MIN_DAYS_AHEAD", 3);
const MAX_DAYS_AHEAD = envInt("MAX_DAYS_AHEAD", 10);
const RUN_ON_START = envBool("RUN_ON_START", true);
const RUN_SCHEDULED = envBool("RUN_SCHEDULED", true);
const SCHEDULE_HOUR_UTC = envInt("SCHEDULE_HOUR_UTC", 2);
const SCHEDULE_MINUTE_UTC = envInt("SCHEDULE_MINUTE_UTC", 0);
const BATCH_LIMIT = 450;

async function runOnce() {
  await seedAirportsIfEmpty();
  await backfillAirportRegionsIfNeeded();

  const airports = await loadAirports();
  if (airports.length < 2) {
    log("Not enough airports to generate flights.");
    return;
  }

  const flightsMin = Math.min(MIN_FLIGHTS, MAX_FLIGHTS);
  const flightsMax = Math.max(MIN_FLIGHTS, MAX_FLIGHTS);
  const daysMin = Math.min(MIN_DAYS_AHEAD, MAX_DAYS_AHEAD);
  const daysMax = Math.max(MIN_DAYS_AHEAD, MAX_DAYS_AHEAD);

  const targetDate = addDays(new Date(), randomInt(daysMin, daysMax));
  const dateKey = formatDateKey(targetDate);
  const targetCount = randomInt(flightsMin, flightsMax);

  const existing = await db.collection("flights").where("dateKey", "==", dateKey).get();
  const usedRoutes = new Set();
  existing.forEach((doc) => {
    const data = doc.data();
    const origin = data.origin;
    const destination = data.destination;
    if (origin && destination) {
      usedRoutes.add(`${origin}_${destination}`);
    } else if (data.routeKey) {
      usedRoutes.add(data.routeKey);
    }
  });

  let created = 0;
  let attempts = 0;
  const maxAttempts = targetCount * 25;

  while (created < targetCount && attempts < maxAttempts) {
    attempts += 1;
    const originAirport = pickOne(airports);
    const destinationAirport = pickOne(airports);
    const origin = originAirport.code;
    const destination = destinationAirport.code;

    if (!origin || !destination || origin === destination) {
      continue;
    }

    const routeKey = `${origin}_${destination}`;
    if (usedRoutes.has(routeKey)) {
      continue;
    }

    const flightId = `${dateKey}_${origin}_${destination}`;
    const flightNumber = randomFlightNumber();
    const aircraft = pickOne(AIRCRAFT_TEMPLATES);
    const airline = pickOne(AIRLINES);
    const stops = randomStops();
    const departure = randomDepartureDateTime(targetDate);
    const arrival = addMinutes(departure, randomInt(60, 240));
    const durationMinutes = Math.max(30, Math.round((arrival - departure) / 60000));
    const checkedBagsIncluded = randomInt(0, 1);
    const emissionsKg = estimateEmissions(durationMinutes, stops);
    // Determine regions for pricing
    const originRegion = regionForAirport(originAirport);
    const destinationRegion = regionForAirport(destinationAirport);
    const basePrice = estimatePrice(durationMinutes, stops);
    const regionMultiplier = regionDistanceMultiplier(originRegion, destinationRegion);
    const price = Math.max(10, Math.round(basePrice * regionMultiplier));

    const seatCapacity = buildSeatCapacity(aircraft);

    const flightPayload = {
      flightNumber,
      flight_number: flightNumber,
      origin,
      destination,
      departureTime: departure.toISOString(),
      arrivalTime: arrival.toISOString(),
      status: "Scheduled",
      dateKey,
      routeKey,
      aircraftId: aircraft.id,
      carryOnIncluded: 1,
      pricingSummary: buildPricingSummary(price, seatCapacity),
      airline,
      stops,
      checkedBagsIncluded,
      emissionsKg,
      durationMinutes,
      generated: true,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    };

    try {
      await db.collection("flights").doc(flightId).create(flightPayload);
    } catch (error) {
      if (error && error.code === 6) {
        continue;
      }
      throw error;
    }

    // if stops, create flight_stops entries
    if (stops > 0) {
      await createFlightStops(flightId, stops, originAirport, destinationAirport, airports, departure);
    }
    usedRoutes.add(routeKey);
    created += 1;
  }

  log(`Generated ${created} flights for ${dateKey}.`);
}

function regionForAirport(airport) {
  if (!airport) return REGION_CODES.EUROPE;
  if (airport.region) return airport.region;
  const country = (airport.country || "").toString();
  const normalized = country.trim().toLowerCase();
  return COUNTRY_REGION_MAP[normalized] || REGION_CODES.EUROPE;
}

async function backfillAirportRegionsIfNeeded() {
  const doBackfill = envBool("BACKFILL_AIRPORT_REGIONS", false);
  if (!doBackfill) return;
  log("Backfilling airport regions into Firestore (BACKFILL_AIRPORT_REGIONS=true)");
  const snapshot = await db.collection("airports").get();
  let batch = db.batch();
  let count = 0;
  for (const doc of snapshot.docs) {
    const data = doc.data();
    if (data && data.region) continue;
    const region = regionForAirport(data || { country: doc.get("country") });
    batch.set(db.collection("airports").doc(doc.id), { region }, { merge: true });
    count += 1;
    if (count >= BATCH_LIMIT) {
      await batch.commit();
      batch = db.batch();
      count = 0;
    }
  }
  if (count > 0) await batch.commit();
}

function regionDistanceMultiplier(originRegion, destinationRegion) {
  if (!originRegion || !destinationRegion) return 1.2;
  if (originRegion === destinationRegion) return 1.0;

  // Simple heuristics
  const americas = [REGION_CODES.NORTH_AMERICA, REGION_CODES.SOUTH_AMERICA];
  const africa = [REGION_CODES.NORTH_AFRICA, REGION_CODES.SOUTHERN_AFRICA];

  if (americas.includes(originRegion) && americas.includes(destinationRegion)) return 1.2;
  if (africa.includes(originRegion) && africa.includes(destinationRegion)) return 1.15;
  if ((originRegion === REGION_CODES.OCEANIA && destinationRegion === REGION_CODES.EAST_ASIA_SE_ASIA)
    || (destinationRegion === REGION_CODES.OCEANIA && originRegion === REGION_CODES.EAST_ASIA_SE_ASIA)) return 1.2;

  // North Africa <-> Europe or Middle East are shorter
  if ((originRegion === REGION_CODES.NORTH_AFRICA && [REGION_CODES.EUROPE, REGION_CODES.MIDDLE_EAST].includes(destinationRegion))
    || (destinationRegion === REGION_CODES.NORTH_AFRICA && [REGION_CODES.EUROPE, REGION_CODES.MIDDLE_EAST].includes(originRegion))) return 1.1;

  // default long-range multiplier
  return 1.45;
}

async function createFlightStops(flightId, stops, originAirport, destinationAirport, airports, departure) {
  // simple approach: pick random intermediate airports (not origin/destination)
  const candidates = airports.filter(a => a.code !== originAirport.code && a.code !== destinationAirport.code);
  const chosen = [];
  for (let i = 0; i < stops; i++) {
    const pick = pickOne(candidates);
    // avoid duplicates
    if (chosen.find(c => c.code === pick.code)) continue;
    chosen.push(pick);
  }

  let currentDeparture = new Date(departure.getTime());
  let seq = 1;
  for (const stop of chosen) {
    const flightSegmentMinutes = randomInt(45, 240);
    const arrival = addMinutes(currentDeparture, flightSegmentMinutes);
    const layover = randomInt(30, 180);
    const departureNext = addMinutes(arrival, layover);
    const docId = `${flightId}_stop_${seq}`;
    await db.collection("flight_stops").doc(docId).set({
      id: docId,
      flightId,
      stopSequence: seq,
      airportCode: stop.code,
      layoverMinutes: layover,
      arrivalTime: arrival.toISOString(),
      departureTime: departureNext.toISOString()
    }, { merge: true });
    seq += 1;
    currentDeparture = departureNext;
  }
}

async function startWorker() {
  if (RUN_ON_START) {
    try {
      await runOnce();
    } catch (error) {
      log(`Run failed: ${error.message || error}`);
    }
  }

  if (!RUN_SCHEDULED) {
    return;
  }

  scheduleNextRun();
}

function scheduleNextRun() {
  const delay = msUntilNextRun();
  log(`Next run in ${Math.round(delay / 60000)} minutes.`);
  setTimeout(async () => {
    try {
      await runOnce();
    } catch (error) {
      log(`Run failed: ${error.message || error}`);
    }
    scheduleNextRun();
  }, delay);
}

async function seedAirportsIfEmpty() {
  const airportsRef = db.collection("airports");
  const sample = await airportsRef.limit(1).get();
  if (!sample.empty) {
    return;
  }

  log("Seeding airports collection.");
  let batch = db.batch();
  let batchCount = 0;

  for (const airport of AIRPORTS) {
    if (!airport.code) {
      continue;
    }
    const docRef = airportsRef.doc(airport.code);
    const region = regionForAirport(airport);
    batch.set(docRef, { ...airport, region }, { merge: true });
    batchCount += 1;

    if (batchCount >= BATCH_LIMIT) {
      await batch.commit();
      batch = db.batch();
      batchCount = 0;
    }
  }

  if (batchCount > 0) {
    await batch.commit();
  }
}

async function loadAirports() {
  const snapshot = await db.collection("airports").get();
  return snapshot.docs
    .map((doc) => ({ ...doc.data(), code: doc.id }))
    .filter((airport) => airport.code);
}

async function writeSeats(seats) {
  let batch = db.batch();
  let count = 0;

  for (const seat of seats) {
    batch.set(db.collection("seats").doc(seat.id), seat.data, { merge: true });
    count += 1;

    if (count >= BATCH_LIMIT) {
      await batch.commit();
      batch = db.batch();
      count = 0;
    }
  }

  if (count > 0) {
    await batch.commit();
  }
}

function buildSeatCapacity(aircraft) {
  const business = (aircraft.businessRows || 0) * (aircraft.seatLetters || []).length;
  const premium = (aircraft.premiumRows || 0) * (aircraft.seatLetters || []).length;
  const economy = (aircraft.economyRows || 0) * (aircraft.seatLetters || []).length;
  return { business, premium, economy, total: business + premium + economy };
}

function buildPricingSummary(basePrice, seatCapacity) {
  return {
    ECONOMY: {
      price: basePrice,
      seatsAvailable: seatCapacity.economy
    },
    PREMIUM_ECONOMY: {
      price: Math.round(basePrice * 1.4),
      seatsAvailable: seatCapacity.premium
    },
    BUSINESS: {
      price: Math.round(basePrice * 2.5),
      seatsAvailable: seatCapacity.business
    },
    meta: {
      currency: "USD",
      totalSeats: seatCapacity.total,
      carryOnIncluded: true
    }
  };
}

function randomFlightNumber() {
  const prefix = pickOne(FLIGHT_PREFIXES);
  return `${prefix}${randomInt(100, 9999)}`;
}

function randomStops() {
  const roll = randomInt(1, 100);
  if (roll <= 70) return 0;
  if (roll <= 90) return 1;
  return 2;
}

function estimateEmissions(durationMinutes, stops) {
  const base = Math.round(durationMinutes * 1.2);
  return base + stops * 40;
}

function estimatePrice(durationMinutes, stops) {
  const base = 50 + durationMinutes * 0.8;
  return Math.round(base + stops * 30);
}

function randomDepartureDateTime(baseDate) {
  const day = new Date(Date.UTC(
    baseDate.getUTCFullYear(),
    baseDate.getUTCMonth(),
    baseDate.getUTCDate()
  ));
  const hour = randomInt(6, 21);
  const minute = pickOne([0, 15, 30, 45]);
  return new Date(Date.UTC(
    day.getUTCFullYear(),
    day.getUTCMonth(),
    day.getUTCDate(),
    hour,
    minute
  ));
}

function addMinutes(date, minutes) {
  return new Date(date.getTime() + minutes * 60000);
}

function addDays(date, days) {
  return new Date(date.getTime() + days * 24 * 60 * 60000);
}

function formatDateKey(date) {
  const year = date.getUTCFullYear();
  const month = String(date.getUTCMonth() + 1).padStart(2, "0");
  const day = String(date.getUTCDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function pickOne(items) {
  return items[randomInt(0, items.length - 1)];
}

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function msUntilNextRun() {
  const now = new Date();
  const next = new Date(Date.UTC(
    now.getUTCFullYear(),
    now.getUTCMonth(),
    now.getUTCDate(),
    SCHEDULE_HOUR_UTC,
    SCHEDULE_MINUTE_UTC,
    0,
    0
  ));

  if (next <= now) {
    next.setUTCDate(next.getUTCDate() + 1);
  }

  return Math.max(0, next.getTime() - now.getTime());
}

function envInt(name, defaultValue) {
  const raw = process.env[name];
  if (!raw) {
    return defaultValue;
  }
  const parsed = Number.parseInt(raw, 10);
  return Number.isFinite(parsed) ? parsed : defaultValue;
}

function envBool(name, defaultValue) {
  const raw = process.env[name];
  if (raw === undefined) {
    return defaultValue;
  }
  return raw.toLowerCase() === "true" || raw === "1" || raw.toLowerCase() === "yes";
}

function log(message) {
  const ts = new Date().toISOString();
  console.log(`[worker] ${ts} ${message}`);
}

startWorker();
