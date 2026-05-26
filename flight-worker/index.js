const admin = require("firebase-admin");

const AIRPORTS = require("./data/airports.json");

const PROJECT_ID = process.env.FIREBASE_PROJECT_ID || undefined;

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId: PROJECT_ID
});

const db = admin.firestore();

const AIRCRAFT_TEMPLATES = [
  {
    id: "A320",
    name: "Airbus A320",
    businessRows: 1,
    premiumRows: 1,
    economyRows: 8,
    seatLetters: ["A", "B", "C", "D"]
  },
  {
    id: "B737",
    name: "Boeing 737",
    businessRows: 1,
    premiumRows: 1,
    economyRows: 7,
    seatLetters: ["A", "B", "C", "D"]
  },
  {
    id: "A220",
    name: "Airbus A220",
    businessRows: 1,
    premiumRows: 1,
    economyRows: 6,
    seatLetters: ["A", "B", "C", "D"]
  },
  {
    id: "E190",
    name: "Embraer 190",
    businessRows: 1,
    premiumRows: 1,
    economyRows: 6,
    seatLetters: ["A", "B", "C", "D"]
  },
  {
    id: "ATR72",
    name: "ATR 72",
    businessRows: 1,
    premiumRows: 1,
    economyRows: 5,
    seatLetters: ["A", "B", "C", "D"]
  }
];

const FLIGHT_PREFIXES = ["AH", "AF", "BA", "DL", "UA", "AA", "EK", "QR", "LH", "AZ"];

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
    const origin = pickOne(airports).code;
    const destination = pickOne(airports).code;

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
    const departure = randomDepartureDateTime(targetDate);
    const arrival = addMinutes(departure, randomInt(60, 240));

    const { seats, counts } = buildSeats(flightId, aircraft);

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
      aircraftType: aircraft.id,
      seatsTotal: seats.length,
      seatsByType: counts,
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

    await writeSeats(seats);
    usedRoutes.add(routeKey);
    created += 1;
  }

  log(`Generated ${created} flights for ${dateKey}.`);
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
    batch.set(docRef, airport, { merge: true });
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

function buildSeats(flightId, aircraft) {
  const seats = [];
  const counts = { business: 0, premium: 0, economy: 0 };
  let rowNumber = 1;

  const addRows = (rows, type, countKey) => {
    for (let i = 0; i < rows; i += 1) {
      for (const letter of aircraft.seatLetters) {
        const seatNumber = `${rowNumber}${letter}`;
        const seatId = `${flightId}_${seatNumber}`;
        seats.push({
          id: seatId,
          data: {
            flightId,
            seatNumber,
            type,
            isOccupied: false
          }
        });
        counts[countKey] += 1;
      }
      rowNumber += 1;
    }
  };

  addRows(aircraft.businessRows, "BUSINESS", "business");
  addRows(aircraft.premiumRows, "PREMIUM", "premium");
  addRows(aircraft.economyRows, "ECONOMY", "economy");

  return { seats, counts };
}

function randomFlightNumber() {
  const prefix = pickOne(FLIGHT_PREFIXES);
  return `${prefix}${randomInt(100, 9999)}`;
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
