const state = {
    snapshot: null,
    airports: [],
    baggage: [],
    gates: [],
    ground: [],
    activeTab: "overview",
    polling: false,
    weatherTouched: false,
    airportsLoading: false,
    airportsError: "",
    airportSearchQuery: "",
    airportSearchTimer: null,
    airportSearchController: null,
    lastBaggageLoad: 0,
    lastAirsideLoad: 0
};

const fmt = new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "medium"
});

const timeFmt = new Intl.DateTimeFormat(undefined, {
    hour: "2-digit",
    minute: "2-digit"
});

const elements = {};

document.addEventListener("DOMContentLoaded", () => {
    bindElements();
    bindEvents();
    highlightMultiplier("x1");
    void refresh();
    void loadAirports();
    window.setInterval(refresh, 1000);
    createIcons();
});

function bindElements() {
    for (const id of [
        "subtitle", "run-state", "clock", "airport-name", "airport-meta", "sim-time",
        "sim-multiplier", "weather-severity", "weather-meta", "ops-total",
        "ops-flow", "airport-position", "airport-detail", "operation-detail",
        "ops-profile", "status-total", "status-grid", "queue-grid", "queue-note",
        "disruption-form", "operations-kpis", "operations-kpi-note", "flight-count",
        "flight-body", "status-filter", "baggage-count", "baggage-body",
        "baggage-status", "baggage-flight", "refresh-baggage", "passenger-note",
        "passenger-grid", "passenger-detail", "gate-count", "gate-body",
        "ground-count", "ground-body", "airport-count", "airport-summary",
        "airport-summary-code", "airport-search", "refresh-airports", "airport-body",
        "weather-current", "weather-source", "fetch-weather", "weather-form",
        "load-weather-form", "count-grid", "events", "event-count", "start-btn",
        "pause-btn", "reset-btn", "reseed-btn"
    ]) {
        elements[toCamel(id)] = document.getElementById(id);
    }
    elements.tabs = [...document.querySelectorAll(".tab")];
    elements.views = [...document.querySelectorAll(".dashboard")];
    elements.multiplierButtons = [...document.querySelectorAll("[data-multiplier]")];
}

function bindEvents() {
    elements.tabs.forEach((tab) => {
        tab.addEventListener("click", () => setTab(tab.dataset.tab));
    });
    elements.startBtn.addEventListener("click", () => mutate("api/control/start"));
    elements.pauseBtn.addEventListener("click", () => mutate("api/control/pause"));
    elements.resetBtn.addEventListener("click", () => mutate("api/control/reset"));
    elements.reseedBtn.addEventListener("click", async () => {
        await mutate("api/import/reseed");
        await loadAirports({force: true});
        await loadActiveDetails(true);
    });
    elements.multiplierButtons.forEach((button) => {
        button.addEventListener("click", () => setMultiplier(button.dataset.multiplier));
    });
    elements.disruptionForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const form = elements.disruptionForm;
        await mutate("api/operations/disruption", {
            type: form.elements.type.value,
            severity: intValue(form.elements.severity),
            durationMinutes: intValue(form.elements.durationMinutes),
            gateCode: form.elements.gateCode.value.trim() || null
        });
        await loadActiveDetails(true);
    });
    elements.statusFilter.addEventListener("change", () => renderFlights(state.snapshot?.flights || []));
    elements.baggageStatus.addEventListener("change", () => loadBaggage(true));
    elements.baggageFlight.addEventListener("input", debounceBaggage);
    elements.refreshBaggage.addEventListener("click", () => loadBaggage(true));
    elements.airportSearch.addEventListener("input", scheduleAirportSearch);
    elements.airportSearch.addEventListener("search", scheduleAirportSearch);
    elements.refreshAirports.addEventListener("click", () => loadAirports({force: true}));
    elements.fetchWeather.addEventListener("click", async () => {
        state.weatherTouched = false;
        await mutate("api/weather/fetch", null, {expectJson: true});
    });
    elements.loadWeatherForm.addEventListener("click", () => {
        state.weatherTouched = false;
        fillWeatherForm(state.snapshot?.weather);
    });
    elements.weatherForm.addEventListener("input", () => {
        state.weatherTouched = true;
    });
    elements.weatherForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        state.weatherTouched = false;
        await mutate("api/weather/manual", weatherPayload(), {expectJson: true});
    });
}

function debounceBaggage() {
    window.clearTimeout(state.baggageTimer);
    state.baggageTimer = window.setTimeout(() => loadBaggage(true), 250);
}

function scheduleAirportSearch() {
    window.clearTimeout(state.airportSearchTimer);
    state.airportSearchTimer = window.setTimeout(() => {
        void loadAirports();
    }, 250);
}

function setMultiplier(multiplier) {
    if (!multiplier) {
        return;
    }
    highlightMultiplier(multiplier);
    void mutate("api/control/multiplier", {multiplier});
}

async function loadAirports({force = false} = {}) {
    const query = elements.airportSearch.value.trim();
    state.airportSearchQuery = query;
    state.airportsLoading = true;
    state.airportsError = "";
    renderAirports();

    if (state.airportSearchController) {
        state.airportSearchController.abort();
    }
    const controller = new AbortController();
    state.airportSearchController = controller;

    const params = new URLSearchParams();
    if (query) {
        params.set("q", query);
    }
    params.set("limit", force ? "80" : "40");

    try {
        const response = await fetch(`api/airports?${params.toString()}`, {
            headers: {"Accept": "application/json"},
            signal: controller.signal
        });
        if (!response.ok) {
            throw new Error(`Airport request failed: ${response.status}`);
        }
        state.airports = await response.json();
    } catch (error) {
        if (error.name !== "AbortError" && state.airportSearchController === controller) {
            state.airports = [];
            state.airportsError = error.message;
            showMessage(error.message);
        }
    } finally {
        if (state.airportSearchController === controller) {
            state.airportSearchController = null;
            state.airportsLoading = false;
            renderAirports();
        }
    }
}

async function refresh() {
    if (state.polling) {
        return;
    }
    state.polling = true;
    try {
        const response = await fetch("api/snapshot", {headers: {"Accept": "application/json"}});
        if (!response.ok) {
            throw new Error(`Snapshot request failed: ${response.status}`);
        }
        state.snapshot = await response.json();
        render(state.snapshot);
        await loadActiveDetails(false);
    } catch (error) {
        showMessage(error.message);
    } finally {
        state.polling = false;
    }
}

async function loadActiveDetails(force) {
    if (state.activeTab === "baggage") {
        await loadBaggage(force);
    }
    if (state.activeTab === "airside") {
        await loadAirside(force);
    }
}

async function loadBaggage(force = false) {
    const now = Date.now();
    if (!force && now - state.lastBaggageLoad < 2500) {
        return;
    }
    state.lastBaggageLoad = now;
    const params = new URLSearchParams();
    params.set("limit", "80");
    if (elements.baggageStatus.value) {
        params.set("status", elements.baggageStatus.value);
    }
    const flight = elements.baggageFlight.value.trim();
    if (flight) {
        params.set("flight", flight);
    }
    try {
        const response = await fetch(`api/operations/baggage?${params.toString()}`, {headers: {"Accept": "application/json"}});
        if (!response.ok) {
            throw new Error(`Baggage request failed: ${response.status}`);
        }
        state.baggage = await response.json();
        renderBaggage();
    } catch (error) {
        showMessage(error.message);
    }
}

async function loadAirside(force = false) {
    const now = Date.now();
    if (!force && now - state.lastAirsideLoad < 2500) {
        return;
    }
    state.lastAirsideLoad = now;
    try {
        const [gatesResponse, groundResponse] = await Promise.all([
            fetch("api/operations/gates", {headers: {"Accept": "application/json"}}),
            fetch("api/operations/ground", {headers: {"Accept": "application/json"}})
        ]);
        if (!gatesResponse.ok || !groundResponse.ok) {
            throw new Error("Airside request failed");
        }
        state.gates = await gatesResponse.json();
        state.ground = await groundResponse.json();
        renderAirside();
    } catch (error) {
        showMessage(error.message);
    }
}

async function mutate(path, body, options = {}) {
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
        if (options.expectJson) {
            await response.json().catch(() => null);
        }
        await refresh();
    } catch (error) {
        showMessage(error.message);
    }
}

function render(snapshot) {
    const operations = snapshot.operations || {};
    elements.clock.textContent = fmt.format(new Date());
    elements.subtitle.textContent = `${number(snapshot.counts.airports)} airports, ${number(snapshot.flights.length)} flights, ${number(operations.passengersTotal)} passengers, ${number(operations.baggageTotal)} bags.`;
    elements.runState.textContent = snapshot.running ? "Running" : "Paused";
    elements.runState.className = `live-chip ${snapshot.running ? "running" : "paused"}`;

    renderOverview(snapshot);
    renderOperations(snapshot);
    renderPassengerDashboard(operations);
    renderAirportSummary(snapshot);
    renderWeather(snapshot.weather, snapshot.airport);
    highlightMultiplier(snapshot.multiplier);
    renderAirports();
    renderFlights(snapshot.flights);
    renderCounts(snapshot.counts);
    renderEvents(snapshot.events);

    if (!state.weatherTouched) {
        fillWeatherForm(snapshot.weather);
    }
    createIcons();
}

function renderOverview(snapshot) {
    const airport = snapshot.airport;
    const weather = snapshot.weather;
    const operations = snapshot.operations || {};
    const statusCounts = groupBy(snapshot.flights, (flight) => flight.status);

    elements.airportName.textContent = airport?.name || "No airport";
    elements.airportMeta.textContent = airport
        ? `${airport.code} | ${airport.city}, ${airport.country} | ${airport.runways} runways`
        : "--";
    elements.simTime.textContent = formatDate(snapshot.simulatedTime);
    elements.simMultiplier.textContent = `Multiplier ${snapshot.multiplier}`;
    elements.weatherSeverity.textContent = weather?.severityLabel || "--";
    elements.weatherMeta.textContent = weather
        ? `${weather.temperatureCelsius.toFixed(1)} C | wind ${weather.windSpeedKmh.toFixed(0)} km/h | visibility ${number(weather.visibilityMeters)} m`
        : "--";
    elements.opsTotal.textContent = `${number(operations.totalFlights)} flights`;
    elements.opsFlow.textContent = `${number(operations.activeFlights)} active | ${number(operations.delayedFlights)} delayed | ${number(operations.gateUtilizationPercent)}% gates`;
    elements.airportPosition.textContent = airport?.latitude != null
        ? `${airport.latitude.toFixed(4)}, ${airport.longitude.toFixed(4)}`
        : "No coordinates";
    elements.opsProfile.textContent = `${operations.trafficProfile || "BUSY"} | target ${number(operations.targetDailyFlights)}`;

    elements.airportDetail.innerHTML = detailItems([
        ["Code", airport?.code],
        ["Type", airport?.type],
        ["City", airport?.city],
        ["Country", airport?.country],
        ["Runways", airport?.runways],
        ["Latitude", airport?.latitude?.toFixed(5)],
        ["Longitude", airport?.longitude?.toFixed(5)]
    ]);
    elements.operationDetail.innerHTML = detailItems([
        ["Passengers", number(operations.passengersTotal)],
        ["Baggage", number(operations.baggageTotal)],
        ["Gate Utilization", `${number(operations.gateUtilizationPercent)}%`],
        ["Runway Queue", number(operations.runwayQueue)],
        ["Baggage Backlog", number(operations.baggageBacklog)],
        ["Delayed Ground Tasks", number(operations.delayedGroundOps)]
    ]);

    elements.statusTotal.textContent = `${number(snapshot.flights.length)} flights`;
    elements.statusGrid.innerHTML = Object.entries(statusCounts).map(([status, count]) => statusCard(status, count)).join("");
}

function renderOperations(snapshot) {
    const operations = snapshot.operations || {};
    elements.queueGrid.innerHTML = [
        ["Runway", operations.runwayQueue],
        ["Check-in", operations.checkInQueue],
        ["Security", operations.securityQueue],
        ["Baggage", operations.baggageBacklog],
        ["Ground Active", operations.activeGroundOps],
        ["Ground Delayed", operations.delayedGroundOps]
    ].map(([label, value]) => statusCard(label, value)).join("");
    elements.operationsKpiNote.textContent = `${operations.trafficProfile || "BUSY"} profile`;
    elements.operationsKpis.innerHTML = countItems([
        ["Target Flights", operations.targetDailyFlights],
        ["Active Flights", operations.activeFlights],
        ["Delayed Flights", operations.delayedFlights],
        ["Passengers", operations.passengersTotal],
        ["Checked In", operations.passengersCheckedIn],
        ["Security Cleared", operations.passengersSecurityCleared],
        ["Boarded", operations.passengersBoarded],
        ["Bags Loaded", operations.bagsLoaded],
        ["Bags Delivered", operations.bagsDelivered],
        ["Delayed Bags", operations.bagsDelayed],
        ["Open Gates", operations.gatesOpen],
        ["Occupied Gates", operations.gatesOccupied]
    ]);
}

function renderPassengerDashboard(operations) {
    elements.passengerGrid.innerHTML = [
        ["Total", operations.passengersTotal],
        ["Checked In", operations.passengersCheckedIn],
        ["Security", operations.passengersSecurityCleared],
        ["Boarded", operations.passengersBoarded],
        ["Missed", operations.passengersMissedConnections],
        ["Check-in Queue", operations.checkInQueue]
    ].map(([label, value]) => statusCard(label, value)).join("");
    elements.passengerDetail.innerHTML = detailItems([
        ["Load Factor", operations.passengerLoadFactor],
        ["Bag Rate", operations.bagRate],
        ["Security Queue", number(operations.securityQueue)],
        ["Runway Queue", number(operations.runwayQueue)],
        ["Active Ground Ops", number(operations.activeGroundOps)],
        ["Delayed Ground Ops", number(operations.delayedGroundOps)]
    ]);
}

function renderAirportSummary(snapshot) {
    const airport = snapshot.airport;
    elements.airportSummaryCode.textContent = airport?.code || "--";
    elements.airportSummary.innerHTML = detailItems([
        ["Code", airport?.code],
        ["Name", airport?.name],
        ["City", airport?.city],
        ["Country", airport?.country],
        ["Type", airport?.type],
        ["Runways", airport?.runways],
        ["Coordinates", airport?.latitude != null ? `${airport.latitude.toFixed(4)}, ${airport.longitude.toFixed(4)}` : null]
    ]);
}

function renderAirports() {
    const activeCode = state.snapshot?.airport?.code;
    if (state.airportsLoading) {
        elements.airportCount.textContent = state.airportSearchQuery ? `Loading ${state.airportSearchQuery}` : "Loading airports";
        elements.airportBody.innerHTML = tableMessageRow("Loading airports", "Fetching imported airport data", 6);
        return;
    }
    if (state.airportsError) {
        elements.airportCount.textContent = "Airport data unavailable";
        elements.airportBody.innerHTML = tableMessageRow("Airport list unavailable", state.airportsError, 6);
        return;
    }
    const airports = state.airports;
    elements.airportCount.textContent = `${number(airports.length)} airports`;
    if (airports.length === 0) {
        elements.airportBody.innerHTML = tableMessageRow("No airports found", "Try another search", 6);
        return;
    }
    elements.airportBody.innerHTML = airports.map((airport) => `
        <tr class="${airport.code === activeCode ? "selected-row" : ""}">
            <td><strong>${escapeHtml(airport.name)}</strong><div class="stat-subtle">${escapeHtml(airport.code)} ${airport.ident ? "| " + escapeHtml(airport.ident) : ""}</div></td>
            <td>${escapeHtml(airport.city)}<div class="stat-subtle">${escapeHtml(airport.country)}</div></td>
            <td>${escapeHtml(airport.type)}</td>
            <td>${airport.latitude == null ? "--" : `${airport.latitude.toFixed(4)}, ${airport.longitude.toFixed(4)}`}</td>
            <td>${number(airport.runways)}</td>
            <td><button class="button small ${airport.code === activeCode ? "primary" : ""}" data-airport-code="${escapeHtml(airport.code)}" type="button">${airport.code === activeCode ? "Selected" : "Select"}</button></td>
        </tr>
    `).join("");
    elements.airportBody.querySelectorAll("[data-airport-code]").forEach((button) => {
        button.addEventListener("click", async () => {
            await mutate("api/airport/select", {code: button.dataset.airportCode});
            await loadActiveDetails(true);
        });
    });
}

function renderWeather(weather, airport) {
    if (!weather) {
        elements.weatherSource.textContent = airport ? `Weather for ${airport.code}` : "Simulation snapshot";
        elements.weatherCurrent.innerHTML = "";
        return;
    }
    elements.weatherSource.textContent = airport ? `Weather for ${airport.code}` : "Simulation snapshot";
    elements.weatherCurrent.innerHTML = `
        <div class="weather-severity ${statusClass(weather.severityCode)}">${escapeHtml(weather.severityLabel)}</div>
        <div class="weather-metrics">
            ${metric("Temp", `${weather.temperatureCelsius.toFixed(1)} C`)}
            ${metric("Feels", `${weather.feelsLikeCelsius.toFixed(1)} C`)}
            ${metric("Wind", `${weather.windSpeedKmh.toFixed(1)} km/h`)}
            ${metric("Gust", `${weather.windGustKmh.toFixed(1)} km/h`)}
            ${metric("Visibility", `${number(weather.visibilityMeters)} m`)}
            ${metric("Cloud", `${weather.cloudCoveragePercent}%`)}
            ${metric("Rain", `${weather.rainMmPerHour.toFixed(1)} mm/h`)}
            ${metric("Snow", `${weather.snowMmPerHour.toFixed(1)} mm/h`)}
            ${metric("Ceiling", `${number(weather.ceilingMeters)} m`)}
            ${metric("Runway", weather.runwaySurface)}
        </div>
    `;
}

function renderFlights(flights) {
    updateStatusFilter(flights);
    const status = elements.statusFilter.value;
    const visibleFlights = status === "ALL" ? flights : flights.filter((flight) => flight.status === status);
    elements.flightCount.textContent = `${number(visibleFlights.length)} of ${number(flights.length)} flights`;
    elements.flightBody.innerHTML = visibleFlights.map((flight) => `
        <tr>
            <td><strong>${escapeHtml(flight.flightNumber)}</strong><div class="stat-subtle">${escapeHtml(flight.airline)}</div><div class="stat-subtle">${escapeHtml(flight.aircraftCode || "--")} ${escapeHtml(flight.aircraftName || "")}</div></td>
            <td>${escapeHtml(flight.originLabel)}<br><span class="stat-subtle">to ${escapeHtml(flight.destinationLabel)}</span><div class="stat-subtle">${escapeHtml(flight.direction || "")} | ${escapeHtml(flight.routeSource || "")}</div></td>
            <td>${formatTime(flight.departureTime)}<div class="stat-subtle">arr ${formatTime(flight.arrivalTime)}</div></td>
            <td><span class="status ${statusClass(flight.status)}">${escapeHtml(flight.status)}</span>${flight.delayReason ? `<div class="stat-subtle">${escapeHtml(flight.delayReason)}</div>` : ""}</td>
            <td>${number(flight.passengerCount)} pax<div class="stat-subtle">${number(flight.baggageCount)} bags</div></td>
            <td>${escapeHtml(flight.gate || "--")}<div class="stat-subtle">${escapeHtml(flight.runway || "")}</div></td>
            <td>
                <div class="control-cell" data-flight-control="${flight.id}">
                    <select data-flight-status>
                        ${flightStatusOptions(flight.status)}
                    </select>
                    <input data-flight-delay type="number" min="0" max="720" value="${flight.delayMinutes || 0}" title="Delay minutes">
                    <input data-flight-reason type="text" value="${escapeHtml(flight.delayReason || "")}" placeholder="Reason">
                    <button class="icon-button primary" data-flight-apply type="button" title="Apply flight control"><i data-lucide="check"></i></button>
                    <button class="icon-button danger" data-flight-cancel type="button" title="Cancel flight"><i data-lucide="ban"></i></button>
                </div>
            </td>
        </tr>
    `).join("");
    elements.flightBody.querySelectorAll("[data-flight-apply]").forEach((button) => {
        button.addEventListener("click", () => applyFlightControl(button.closest("[data-flight-control]")));
    });
    elements.flightBody.querySelectorAll("[data-flight-cancel]").forEach((button) => {
        button.addEventListener("click", () => cancelFlight(button.closest("[data-flight-control]")));
    });
    createIcons();
}

async function applyFlightControl(container) {
    await mutate(`api/flights/${container.dataset.flightControl}/control`, {
        status: container.querySelector("[data-flight-status]").value,
        delayMinutes: intValue(container.querySelector("[data-flight-delay]")),
        reason: container.querySelector("[data-flight-reason]").value.trim() || "Manual operations control"
    });
}

async function cancelFlight(container) {
    await mutate(`api/flights/${container.dataset.flightControl}/control`, {
        status: "CANCELLED",
        delayMinutes: intValue(container.querySelector("[data-flight-delay]")),
        reason: "Cancelled by operations control"
    });
}

function renderBaggage() {
    elements.baggageCount.textContent = `${number(state.baggage.length)} shown`;
    if (state.baggage.length === 0) {
        elements.baggageBody.innerHTML = tableMessageRow("No bags match", "Change the filters or refresh", 6);
        return;
    }
    elements.baggageBody.innerHTML = state.baggage.map((bag) => `
        <tr>
            <td><strong>${escapeHtml(bag.tag)}</strong><div class="stat-subtle">ID ${bag.id}</div></td>
            <td>${escapeHtml(bag.flightNumber)}<div class="stat-subtle">Flight ${bag.flightId}</div></td>
            <td><span class="status ${statusClass(bag.status)}">${escapeHtml(bag.status)}</span></td>
            <td>${escapeHtml(bag.belt || "--")}</td>
            <td>${escapeHtml(bag.exceptionReason || "--")}</td>
            <td>${formatDate(bag.lastUpdated)}</td>
        </tr>
    `).join("");
}

function renderAirside() {
    elements.gateCount.textContent = `${number(state.gates.length)} gates`;
    elements.gateBody.innerHTML = state.gates.length === 0
        ? tableMessageRow("No gates loaded", "Reset or reseed the simulation", 5)
        : state.gates.map((gate) => `
            <tr>
                <td><strong>${escapeHtml(gate.gateCode)}</strong><div class="stat-subtle">${escapeHtml(gate.terminal)}</div></td>
                <td><span class="status ${statusClass(gate.state)}">${escapeHtml(gate.open ? gate.state : "CLOSED")}</span></td>
                <td>${escapeHtml(gate.flightNumber || "--")}</td>
                <td>${number(gate.passengerQueue)} pax<div class="stat-subtle">${number(gate.baggageQueue)} bags</div></td>
                <td><button class="button small" data-gate-toggle="${escapeHtml(gate.gateCode)}" data-gate-open="${gate.open ? "false" : "true"}" type="button">${gate.open ? "Close" : "Open"}</button></td>
            </tr>
        `).join("");
    elements.gateBody.querySelectorAll("[data-gate-toggle]").forEach((button) => {
        button.addEventListener("click", async () => {
            await mutate("api/operations/disruption", {
                type: button.dataset.gateOpen === "true" ? "GATE_OPEN" : "GATE_CLOSE",
                severity: 2,
                durationMinutes: 20,
                gateCode: button.dataset.gateToggle
            });
            await loadAirside(true);
        });
    });

    elements.groundCount.textContent = `${number(state.ground.length)} tasks`;
    elements.groundBody.innerHTML = state.ground.length === 0
        ? tableMessageRow("No ground tasks", "Reset or reseed the simulation", 5)
        : state.ground.map((task) => `
            <tr>
                <td><strong>${escapeHtml(task.operationType)}</strong><div class="stat-subtle">Delay ${number(task.delayMinutes)} min</div></td>
                <td>${escapeHtml(task.flightNumber || "--")}</td>
                <td>${escapeHtml(task.gateCode || "--")}</td>
                <td><span class="status ${statusClass(task.status)}">${escapeHtml(task.status)}</span></td>
                <td>${formatTime(task.dueAt)}</td>
            </tr>
        `).join("");
}

function renderCounts(counts) {
    elements.countGrid.innerHTML = countItems([
        ["Countries", counts.countries],
        ["Regions", counts.regions],
        ["Airports", counts.airports],
        ["Runways", counts.runways],
        ["Navaids", counts.navaids],
        ["Weather Snapshots", counts.weatherSnapshots],
        ["Fare Templates", counts.flightTemplates],
        ["OpenFlights Airlines", counts.openFlightAirlines],
        ["OpenFlights Routes", counts.openFlightRoutes],
        ["OpenFlights Planes", counts.openFlightPlanes],
        ["Simulation Flights", counts.demoFlights],
        ["Passengers", counts.passengers],
        ["Baggage", counts.baggage],
        ["Gates", counts.gates],
        ["Ground Ops", counts.groundOperations],
        ["Events", counts.events]
    ]);
}

function renderEvents(events) {
    elements.eventCount.textContent = `${number(events.length)} events`;
    elements.events.innerHTML = events.map((event) => `
        <article class="event">
            <div class="event-top">
                <span>${escapeHtml(event.category)} | ${escapeHtml(event.level)}</span>
                <span>${formatDate(event.occurredAt)}</span>
            </div>
            <div class="event-message">${escapeHtml(event.message)}</div>
        </article>
    `).join("");
}

function fillWeatherForm(weather) {
    if (!weather) {
        return;
    }
    const form = elements.weatherForm;
    for (const [name, value] of Object.entries({
        temperatureCelsius: weather.temperatureCelsius,
        feelsLikeCelsius: weather.feelsLikeCelsius,
        windSpeedKmh: weather.windSpeedKmh,
        windGustKmh: weather.windGustKmh,
        windDirectionDegrees: weather.windDirectionDegrees,
        rainMmPerHour: weather.rainMmPerHour,
        snowMmPerHour: weather.snowMmPerHour,
        visibilityMeters: weather.visibilityMeters,
        cloudCoveragePercent: weather.cloudCoveragePercent,
        ceilingMeters: weather.ceilingMeters,
        cloudLabel: weather.cloudLabel,
        runwaySurface: weather.runwaySurface,
        severityCode: weather.severityCode
    })) {
        form.elements[name].value = value ?? "";
    }
    form.elements.fog.checked = Boolean(weather.fog);
    form.elements.thunderstorm.checked = Boolean(weather.thunderstorm);
    form.elements.hail.checked = Boolean(weather.hail);
}

function weatherPayload() {
    const form = elements.weatherForm;
    return {
        temperatureCelsius: numberValue(form.elements.temperatureCelsius),
        feelsLikeCelsius: numberValue(form.elements.feelsLikeCelsius),
        windSpeedKmh: numberValue(form.elements.windSpeedKmh),
        windGustKmh: numberValue(form.elements.windGustKmh),
        windDirectionDegrees: intValue(form.elements.windDirectionDegrees),
        rainMmPerHour: numberValue(form.elements.rainMmPerHour),
        snowMmPerHour: numberValue(form.elements.snowMmPerHour),
        hail: form.elements.hail.checked,
        thunderstorm: form.elements.thunderstorm.checked,
        visibilityMeters: intValue(form.elements.visibilityMeters),
        fog: form.elements.fog.checked,
        cloudCoveragePercent: intValue(form.elements.cloudCoveragePercent),
        ceilingMeters: intValue(form.elements.ceilingMeters),
        cloudLabel: form.elements.cloudLabel.value,
        runwaySurface: form.elements.runwaySurface.value,
        severityCode: form.elements.severityCode.value
    };
}

function setTab(tabName) {
    state.activeTab = tabName;
    elements.tabs.forEach((tab) => tab.classList.toggle("active", tab.dataset.tab === tabName));
    elements.views.forEach((view) => view.classList.toggle("active", view.id === `view-${tabName}`));
    if (tabName === "airports" && !state.airportsLoading && state.airports.length === 0 && !state.airportsError) {
        void loadAirports();
    }
    void loadActiveDetails(true);
}

function highlightMultiplier(multiplier) {
    elements.multiplierButtons.forEach((button) => {
        const active = button.dataset.multiplier === multiplier;
        button.classList.toggle("active", active);
        button.setAttribute("aria-pressed", String(active));
    });
}

function updateStatusFilter(flights) {
    const current = elements.statusFilter.value;
    const statuses = [...new Set(flights.map((flight) => flight.status))].sort();
    elements.statusFilter.innerHTML = `<option value="ALL">All statuses</option>` + statuses.map((status) => `
        <option value="${escapeHtml(status)}">${escapeHtml(status)}</option>
    `).join("");
    elements.statusFilter.value = statuses.includes(current) ? current : "ALL";
}

function flightStatusOptions(current) {
    return ["SCHEDULED", "CHECK_IN_OPEN", "BOARDING", "DELAYED", "DEPARTED", "ARRIVED", "CANCELLED"]
        .map((status) => `<option value="${status}" ${status === current ? "selected" : ""}>${status}</option>`)
        .join("");
}

function countItems(items) {
    return items.map(([label, value]) => `
        <div class="count-card">
            <span>${escapeHtml(label)}</span>
            <strong>${number(value)}</strong>
        </div>
    `).join("");
}

function statusCard(label, value) {
    return `
        <div class="status-card">
            <span class="status ${statusClass(label)}">${escapeHtml(label)}</span>
            <strong>${number(value)}</strong>
        </div>
    `;
}

function tableMessageRow(title, message, columns) {
    return `
        <tr>
            <td class="table-state" colspan="${columns}">
                <strong>${escapeHtml(title)}</strong>
                <span>${escapeHtml(message)}</span>
            </td>
        </tr>
    `;
}

function detailItems(items) {
    return items.map(([label, value]) => `
        <div class="detail-item">
            <span>${escapeHtml(label)}</span>
            <strong>${escapeHtml(value ?? "--")}</strong>
        </div>
    `).join("");
}

function metric(label, value) {
    return `<div class="metric"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value)}</strong></div>`;
}

function groupBy(items, keyFn) {
    return items.reduce((acc, item) => {
        const key = keyFn(item) || "UNKNOWN";
        acc[key] = (acc[key] || 0) + 1;
        return acc;
    }, {});
}

function statusClass(status) {
    return `status-${String(status || "scheduled").toLowerCase().replaceAll("_", "-")}`;
}

function formatDate(value) {
    return value ? fmt.format(new Date(value)) : "--";
}

function formatTime(value) {
    return value ? timeFmt.format(new Date(value)) : "--";
}

function number(value) {
    return Number(value ?? 0).toLocaleString();
}

function numberValue(input) {
    return Number(input.value || 0);
}

function intValue(input) {
    return Math.trunc(Number(input.value || 0));
}

function showMessage(message) {
    elements.subtitle.textContent = message;
}

async function safeJson(response) {
    try {
        return await response.json();
    } catch {
        return null;
    }
}

function toCamel(id) {
    return id.replace(/-([a-z])/g, (_, char) => char.toUpperCase());
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}

function createIcons() {
    if (window.lucide) {
        window.lucide.createIcons();
    }
}
