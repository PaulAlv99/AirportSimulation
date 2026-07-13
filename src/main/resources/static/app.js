const state = {
    snapshot: null,
    airports: [],
    activeTab: "overview",
    polling: false,
    weatherTouched: false
};

const fmt = new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "medium"
});

const elements = {};

document.addEventListener("DOMContentLoaded", () => {
    bindElements();
    bindEvents();
    refreshAll();
    window.setInterval(refresh, 1000);
    if (window.lucide) {
        window.lucide.createIcons();
    }
});

function bindElements() {
    for (const id of [
        "subtitle", "run-state", "clock", "airport-name", "airport-meta", "sim-time",
        "sim-multiplier", "weather-severity", "weather-meta", "flight-total",
        "flight-flow", "airport-position", "airport-detail", "weather-observed",
        "weather-detail", "status-total", "status-grid", "airport-count",
        "airport-search", "refresh-airports", "airport-body", "weather-current",
        "fetch-weather", "weather-form", "load-weather-form", "flight-count",
        "flight-body", "status-filter", "count-grid", "events", "event-count",
        "multiplier", "start-btn", "pause-btn", "reset-btn", "reseed-btn",
        "apply-multiplier"
    ]) {
        elements[toCamel(id)] = document.getElementById(id);
    }
    elements.tabs = [...document.querySelectorAll(".tab")];
    elements.views = [...document.querySelectorAll(".dashboard")];
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
        await loadAirports();
    });
    elements.applyMultiplier.addEventListener("click", () => mutate("api/control/multiplier", {
        multiplier: elements.multiplier.value
    }));
    elements.airportSearch.addEventListener("input", renderAirports);
    elements.refreshAirports.addEventListener("click", loadAirports);
    elements.statusFilter.addEventListener("change", () => renderFlights(state.snapshot?.flights || []));
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

async function refreshAll() {
    await Promise.all([loadAirports(), refresh()]);
}

async function loadAirports() {
    try {
        const response = await fetch("api/airports", {headers: {"Accept": "application/json"}});
        if (!response.ok) {
            throw new Error(`Airport request failed: ${response.status}`);
        }
        state.airports = await response.json();
        renderAirports();
    } catch (error) {
        showMessage(error.message);
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
    } catch (error) {
        showMessage(error.message);
    } finally {
        state.polling = false;
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
    elements.clock.textContent = fmt.format(new Date());
    elements.subtitle.textContent = `${number(snapshot.counts.airports)} airports, ${number(snapshot.flights.length)} active simulation flights, ${snapshot.airport?.code || "no airport"} selected.`;
    elements.runState.textContent = snapshot.running ? "Running" : "Paused";
    elements.runState.className = `live-chip ${snapshot.running ? "running" : "paused"}`;
    elements.multiplier.value = snapshot.multiplier;

    renderOverview(snapshot);
    renderWeather(snapshot.weather);
    renderFlights(snapshot.flights);
    renderCounts(snapshot.counts);
    renderEvents(snapshot.events);

    if (!state.weatherTouched) {
        fillWeatherForm(snapshot.weather);
    }
    if (window.lucide) {
        window.lucide.createIcons();
    }
}

function renderOverview(snapshot) {
    const airport = snapshot.airport;
    const weather = snapshot.weather;
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
    elements.flightTotal.textContent = `${number(snapshot.flights.length)} flights`;
    elements.flightFlow.textContent = flightFlowText(statusCounts);
    elements.airportPosition.textContent = airport?.latitude != null
        ? `${airport.latitude.toFixed(4)}, ${airport.longitude.toFixed(4)}`
        : "No coordinates";
    elements.weatherObserved.textContent = weather ? formatDate(weather.observedAt) : "--";

    elements.airportDetail.innerHTML = detailItems([
        ["Code", airport?.code],
        ["Type", airport?.type],
        ["City", airport?.city],
        ["Country", airport?.country],
        ["Runways", airport?.runways],
        ["Latitude", airport?.latitude?.toFixed(5)],
        ["Longitude", airport?.longitude?.toFixed(5)]
    ]);
    elements.weatherDetail.innerHTML = detailItems([
        ["Temperature", `${weather?.temperatureCelsius?.toFixed(1) ?? "--"} C`],
        ["Feels Like", `${weather?.feelsLikeCelsius?.toFixed(1) ?? "--"} C`],
        ["Wind", `${weather?.windSpeedKmh?.toFixed(1) ?? "--"} km/h`],
        ["Gust", `${weather?.windGustKmh?.toFixed(1) ?? "--"} km/h`],
        ["Direction", `${weather?.windDirectionDegrees ?? "--"} deg`],
        ["Rain", `${weather?.rainMmPerHour?.toFixed(1) ?? "--"} mm/h`],
        ["Snow", `${weather?.snowMmPerHour?.toFixed(1) ?? "--"} mm/h`],
        ["Clouds", `${weather?.cloudCoveragePercent ?? "--"}%`],
        ["Ceiling", `${number(weather?.ceilingMeters)} m`],
        ["Runway", weather?.runwaySurface]
    ]);

    elements.statusTotal.textContent = `${number(snapshot.flights.length)} flights`;
    elements.statusGrid.innerHTML = Object.entries(statusCounts).map(([status, count]) => `
        <div class="status-card">
            <span class="status ${statusClass(status)}">${escapeHtml(status)}</span>
            <strong>${number(count)}</strong>
        </div>
    `).join("");
}

function renderAirports() {
    const activeCode = state.snapshot?.airport?.code;
    const query = elements.airportSearch.value.trim().toLowerCase();
    const airports = state.airports.filter((airport) => {
        if (!query) {
            return true;
        }
        return [airport.code, airport.ident, airport.name, airport.city, airport.country, airport.type]
            .some((value) => String(value || "").toLowerCase().includes(query));
    });
    elements.airportCount.textContent = `${number(airports.length)} of ${number(state.airports.length)} airports`;
    elements.airportBody.innerHTML = airports.map((airport) => `
        <tr class="${airport.code === activeCode ? "selected-row" : ""}">
            <td><strong>${escapeHtml(airport.name)}</strong><div class="stat-subtle">${escapeHtml(airport.code)} ${airport.ident ? "| " + escapeHtml(airport.ident) : ""}</div></td>
            <td>${escapeHtml(airport.city)}<div class="stat-subtle">${escapeHtml(airport.country)}</div></td>
            <td>${escapeHtml(airport.type)}</td>
            <td>${airport.latitude == null ? "--" : `${airport.latitude.toFixed(4)}, ${airport.longitude.toFixed(4)}`}</td>
            <td>${number(airport.runways)}</td>
            <td><button class="button small" data-airport-code="${escapeHtml(airport.code)}" type="button">${airport.code === activeCode ? "Active" : "Select"}</button></td>
        </tr>
    `).join("");
    elements.airportBody.querySelectorAll("[data-airport-code]").forEach((button) => {
        button.addEventListener("click", () => mutate("api/airport/select", {code: button.dataset.airportCode}));
    });
}

function renderWeather(weather) {
    if (!weather) {
        elements.weatherCurrent.innerHTML = "";
        return;
    }
    elements.weatherCurrent.innerHTML = `
        <div class="weather-severity ${weather.severityCode.toLowerCase()}">${escapeHtml(weather.severityLabel)}</div>
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
    const visibleFlights = status === "ALL"
        ? flights
        : flights.filter((flight) => flight.status === status);
    elements.flightCount.textContent = `${number(visibleFlights.length)} of ${number(flights.length)} flights`;
    elements.flightBody.innerHTML = visibleFlights.map((flight) => `
        <tr>
            <td><strong>${escapeHtml(flight.flightNumber)}</strong><div class="stat-subtle">${escapeHtml(flight.airline)}</div></td>
            <td>${escapeHtml(flight.originLabel)}<br><span class="stat-subtle">to ${escapeHtml(flight.destinationLabel)}</span></td>
            <td>${formatDate(flight.departureTime)}</td>
            <td>${formatDate(flight.arrivalTime)}</td>
            <td><span class="status ${statusClass(flight.status)}">${escapeHtml(flight.status)}</span></td>
            <td>${escapeHtml(flight.gate || "--")}<div class="stat-subtle">${escapeHtml(flight.runway || "")}</div></td>
            <td>${flight.delayMinutes ? `${flight.delayMinutes} min` : "--"}${flight.weatherNotes ? `<div class="stat-subtle">${escapeHtml(flight.weatherNotes)}</div>` : ""}</td>
        </tr>
    `).join("");
}

function renderCounts(counts) {
    const items = [
        ["Countries", counts.countries],
        ["Regions", counts.regions],
        ["Airports", counts.airports],
        ["Runways", counts.runways],
        ["Navaids", counts.navaids],
        ["Weather Snapshots", counts.weatherSnapshots],
        ["Flight Templates", counts.flightTemplates],
        ["Simulation Flights", counts.demoFlights],
        ["Events", counts.events]
    ];
    elements.countGrid.innerHTML = items.map(([label, value]) => `
        <div class="count-card">
            <span>${label}</span>
            <strong>${number(value)}</strong>
        </div>
    `).join("");
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
}

function updateStatusFilter(flights) {
    const current = elements.statusFilter.value;
    const statuses = [...new Set(flights.map((flight) => flight.status))].sort();
    elements.statusFilter.innerHTML = `<option value="ALL">All statuses</option>` + statuses.map((status) => `
        <option value="${escapeHtml(status)}">${escapeHtml(status)}</option>
    `).join("");
    elements.statusFilter.value = statuses.includes(current) ? current : "ALL";
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

function flightFlowText(statusCounts) {
    const departed = statusCounts.DEPARTED || 0;
    const boarding = statusCounts.BOARDING || 0;
    const delayed = statusCounts.DELAYED || 0;
    return `${departed} departed | ${boarding} boarding | ${delayed} delayed`;
}

function statusClass(status) {
    return `status-${String(status || "scheduled").toLowerCase().replaceAll("_", "-")}`;
}

function formatDate(value) {
    return value ? fmt.format(new Date(value)) : "--";
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
