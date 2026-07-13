const state = {
    lastSnapshot: null,
    polling: false
};

const fmt = new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "medium"
});

const elements = {};

document.addEventListener("DOMContentLoaded", () => {
    elements.subtitle = document.getElementById("subtitle");
    elements.runState = document.getElementById("run-state");
    elements.clock = document.getElementById("clock");
    elements.airportName = document.getElementById("airport-name");
    elements.airportMeta = document.getElementById("airport-meta");
    elements.simTime = document.getElementById("sim-time");
    elements.simMultiplier = document.getElementById("sim-multiplier");
    elements.weatherSeverity = document.getElementById("weather-severity");
    elements.weatherMeta = document.getElementById("weather-meta");
    elements.countGrid = document.getElementById("count-grid");
    elements.flightBody = document.getElementById("flight-body");
    elements.flightCount = document.getElementById("flight-count");
    elements.events = document.getElementById("events");
    elements.eventCount = document.getElementById("event-count");
    elements.multiplier = document.getElementById("multiplier");
    elements.startBtn = document.getElementById("start-btn");
    elements.pauseBtn = document.getElementById("pause-btn");
    elements.resetBtn = document.getElementById("reset-btn");
    elements.reseedBtn = document.getElementById("reseed-btn");
    elements.applyMultiplier = document.getElementById("apply-multiplier");

    bindEvents();
    refresh();
    window.setInterval(refresh, 1000);
    window.setInterval(() => {
        if (state.polling || !state.lastSnapshot) {
            return;
        }
        refresh();
    }, 5000);
    if (window.lucide) {
        window.lucide.createIcons();
    }
});

function bindEvents() {
    elements.startBtn.addEventListener("click", () => mutate("api/control/start"));
    elements.pauseBtn.addEventListener("click", () => mutate("api/control/pause"));
    elements.resetBtn.addEventListener("click", () => mutate("api/control/reset"));
    elements.reseedBtn.addEventListener("click", () => mutate("api/import/reseed"));
    elements.applyMultiplier.addEventListener("click", () => mutate("api/control/multiplier", {
        multiplier: elements.multiplier.value
    }));
}

async function refresh() {
    if (state.polling) {
        return;
    }
    state.polling = true;
    try {
        const response = await fetch("api/snapshot", {
            headers: {
                "Accept": "application/json"
            }
        });
        if (!response.ok) {
            throw new Error(`Snapshot request failed: ${response.status}`);
        }
        const snapshot = await response.json();
        state.lastSnapshot = snapshot;
        render(snapshot);
    } catch (error) {
        elements.subtitle.textContent = error.message;
    } finally {
        state.polling = false;
    }
}

async function mutate(path, body) {
    try {
        const response = await fetch(path, {
            method: "POST",
            headers: body ? {"Content-Type": "application/json"} : undefined,
            body: body ? JSON.stringify(body) : undefined
        });
        if (!response.ok && response.status !== 204) {
            const detail = await safeJson(response);
            throw new Error(detail?.detail || detail?.title || `Request failed: ${response.status}`);
        }
        await refresh();
    } catch (error) {
        elements.subtitle.textContent = error.message;
    }
}

async function safeJson(response) {
    try {
        return await response.json();
    } catch {
        return null;
    }
}

function render(snapshot) {
    elements.subtitle.textContent = `${snapshot.counts.airports.toLocaleString()} airports loaded, ${snapshot.flights.length.toLocaleString()} live flights running.`;
    elements.runState.textContent = snapshot.running ? "Running" : "Paused";
    elements.runState.className = `live-chip ${snapshot.running ? "running" : "paused"}`;
    elements.clock.textContent = fmt.format(new Date());

    elements.airportName.textContent = snapshot.airport
        ? snapshot.airport.name
        : "No airport loaded";
    elements.airportMeta.textContent = snapshot.airport
        ? `${snapshot.airport.code} · ${snapshot.airport.city}, ${snapshot.airport.country} · ${snapshot.airport.runways} runways`
        : "Import data not initialized";

    elements.simTime.textContent = snapshot.simulatedTime ? formatDate(snapshot.simulatedTime) : "--";
    elements.simMultiplier.textContent = `Multiplier ${snapshot.multiplier}`;

    elements.weatherSeverity.textContent = snapshot.weather
        ? snapshot.weather.severityLabel
        : "--";
    elements.weatherMeta.textContent = snapshot.weather
        ? `${snapshot.weather.temperatureCelsius.toFixed(0)}°C, wind ${snapshot.weather.windSpeedKmh.toFixed(0)} km/h, visibility ${snapshot.weather.visibilityMeters.toLocaleString()} m`
        : "--";

    renderCounts(snapshot.counts);
    renderFlights(snapshot.flights);
    renderEvents(snapshot.events);
}

function renderCounts(counts) {
    const items = [
        ["Countries", counts.countries],
        ["Regions", counts.regions],
        ["Airports", counts.airports],
        ["Runways", counts.runways],
        ["Navaids", counts.navaids],
        ["Weather", counts.weatherSnapshots],
        ["Flight Rows", counts.flightTemplates],
        ["Demo Flights", counts.demoFlights]
    ];

    elements.countGrid.innerHTML = items.map(([label, value]) => `
        <div class="count-card">
            <div class="label">${label}</div>
            <div class="value">${Number(value).toLocaleString()}</div>
        </div>
    `).join("");
}

function renderFlights(flights) {
    elements.flightCount.textContent = `${flights.length} flights`;
    elements.flightBody.innerHTML = flights.map((flight) => `
        <tr>
            <td><strong>${escapeHtml(flight.flightNumber)}</strong><div class="stat-subtle">${escapeHtml(flight.airline)}</div></td>
            <td>${escapeHtml(flight.originLabel)}<br><span class="stat-subtle">to ${escapeHtml(flight.destinationLabel)}</span></td>
            <td>${formatDate(flight.departureTime)}</td>
            <td>${formatDate(flight.arrivalTime)}</td>
            <td><span class="status status-${statusClass(flight.status)}">${escapeHtml(flight.status)}</span></td>
            <td>${escapeHtml(flight.gate || "—")}<div class="stat-subtle">${escapeHtml(flight.runway || "")}</div></td>
            <td>${flight.delayMinutes ? `${flight.delayMinutes} min` : "—"}${flight.weatherNotes ? `<div class="stat-subtle">${escapeHtml(flight.weatherNotes)}</div>` : ""}</td>
        </tr>
    `).join("");
}

function renderEvents(events) {
    elements.eventCount.textContent = `${events.length} events`;
    elements.events.innerHTML = events.map((event) => `
        <article class="event">
            <div class="event-top">
                <span>${escapeHtml(event.category)} · ${escapeHtml(event.level)}</span>
                <span>${formatDate(event.occurredAt)}</span>
            </div>
            <div class="event-message">${escapeHtml(event.message)}</div>
        </article>
    `).join("");
}

function statusClass(status) {
    return String(status || "scheduled").toLowerCase().replace(/\s+/g, "-");
}

function formatDate(value) {
    if (!value) {
        return "—";
    }
    return fmt.format(new Date(value));
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}
