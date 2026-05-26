# Flight Generator Worker

## What it does
- Seeds the `airports` collection (only if empty) using `data/airports.json`.
- Creates a few flights per day for dates 3-10 days ahead.
- Ensures only one route per day by using `dateKey` + `routeKey`.
- Creates seats for each flight using `BUSINESS`, `PREMIUM`, and `ECONOMY` types.

## Where it runs
This worker runs on your VM (Docker). It does not use Firebase Cloud Functions.

## Requirements
- Docker + Docker Compose on the VM
- A Firebase service account JSON with Firestore access (not the Android google-services.json)

## Setup (VM)
1) Copy the `flight-worker/` folder to the VM.
2) Enter the folder:
   - `cd flight-worker`
3) Create a secrets folder and place your service account JSON there:
   - `mkdir -p secrets`
   - Copy the service account JSON file to `secrets/serviceAccount.json`
4) Update `docker-compose.worker.yml` with your Firebase project id if needed.
5) Start the worker:
   - `docker compose -f docker-compose.worker.yml up -d --build`

## Useful commands
- View logs: `docker compose -f docker-compose.worker.yml logs -f`
- Stop: `docker compose -f docker-compose.worker.yml down`
- Run once and exit: `docker compose -f docker-compose.worker.yml run --rm -e RUN_SCHEDULED=false -e RUN_ON_START=true flight-worker`

## Configuration
All settings are environment variables in `docker-compose.worker.yml` (same folder).

- `MIN_FLIGHTS` / `MAX_FLIGHTS`: flights created per run (default 2-5).
- `MIN_DAYS_AHEAD` / `MAX_DAYS_AHEAD`: flight dates in the future (default 3-10).
- `RUN_ON_START`: run once immediately when the container starts.
- `RUN_SCHEDULED`: keep running daily at the scheduled time.
- `SCHEDULE_HOUR_UTC` / `SCHEDULE_MINUTE_UTC`: daily schedule time in UTC.

To run only once and exit, set:
- `RUN_ON_START=true`
- `RUN_SCHEDULED=false`

## Collections and fields
### airports
- `code` (document id)
- `name`
- `city`
- `country`

### flights
- `flightNumber` and `flight_number`
- `origin`, `destination`
- `departureTime`, `arrivalTime` (ISO strings)
- `status`
- `dateKey` (YYYY-MM-DD)
- `routeKey` (origin_destination)
- `aircraftType`
- `seatsTotal`
- `seatsByType` (object)
- `generated` (boolean)
- `createdAt` (server timestamp)

### seats
- `flightId`
- `seatNumber`
- `type` (BUSINESS, PREMIUM, ECONOMY)
- `isOccupied` (boolean)

## Firestore limits
The default settings are intentionally small to stay under free limits. If you increase flights or seat counts, keep daily writes under your quota.
