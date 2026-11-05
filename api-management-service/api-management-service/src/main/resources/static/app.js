(function () {
    "use strict";

    const SESSION_KEY = "gatekeeper-control-plane-session";

    const state = {
        token: null,
        username: null,
        role: null,
        organizationId: null,
        organizations: [],
        routes: [],
        organization: null,
        audits: [],
        currentSection: "overview"
    };

    const $ = id => document.getElementById(id);

    document.addEventListener("DOMContentLoaded", () => {
        bindEvents();
        restoreSession();
    });

    // -------------------------------
    // EVENT LISTENERS
    // -------------------------------

    function bindEvents() {
        // Authentication
        $("loginForm").addEventListener("submit", login);

        $("togglePassword").addEventListener("click", () => {
            const input = $("password");
            const showing = input.type === "text";
            input.type = showing ? "password" : "text";
            $("togglePassword").textContent = showing ? "Show" : "Hide";
        });

        $("logoutButton").addEventListener("click", logout);

        $("mobileMenuButton").addEventListener("click", () => {
            $("sidebar").classList.toggle("open");
        });

        // Organizations
        $("createOrganizationButton").addEventListener("click", () => openOrganizationDialog());
        $("refreshOrganizationsButton").addEventListener("click", () => loadOrganizations());
        $("organizationSearch").addEventListener("input", renderOrganizations);
        $("organizationsTableBody").addEventListener("click", handleOrganizationAction);
        $("organizationsTableBody").addEventListener("change", handleOrganizationStatus);
        $("organizationForm").addEventListener("submit", saveOrganization);

        // Tenant administrator dialog
        $("tenantAdminForm").addEventListener("submit", saveTenantAdmin);

        $("closeTenantAdminDialog").addEventListener("click", () => {
            $("tenantAdminDialog").close();
        });

        $("cancelTenantAdminDialog").addEventListener("click", () => {
            $("tenantAdminDialog").close();
        });

        $("deleteTenantAdminButton").addEventListener("click", deleteTenantAdmin);

        // Routes
        $("createRouteButton").addEventListener("click", () => openRouteDialog());
        $("refreshRoutesButton").addEventListener("click", () => loadRoutes());
        $("routeSearch").addEventListener("input", renderRoutes);
        $("routesTableBody").addEventListener("click", handleRouteAction);
        $("routeForm").addEventListener("submit", saveRoute);
        $("addHeaderRuleButton").addEventListener("click", () => addHeaderRule());
        $("headerRulesContainer").addEventListener("click", removeHeaderRule);

        // Audit records
        $("refreshAuditButton").addEventListener("click", () => loadAudits());

        // Other dialogs
        document.querySelectorAll(".close-dialog").forEach(button => {
            button.addEventListener("click", () => {
                button.closest("dialog").close();
            });
        });

        // Close dialogs by clicking their backdrop
        document.querySelectorAll("dialog").forEach(dialog => {
            dialog.addEventListener("click", event => {
                if (event.target === dialog) dialog.close();
            });
        });
    }

    // -------------------------------
    // AUTHENTICATION
    // -------------------------------

    async function login(event) {
        event.preventDefault();

        const button = $("loginButton");
        const errorElement = $("loginError");

        button.disabled = true;
        button.textContent = "Signing in...";
        errorElement.classList.add("hidden");

        const username = $("username").value.trim();
        const password = $("password").value;

        try {
            const response = await api("/api/auth/login", {
                method: "POST",
                publicRequest: true,
                body: { username, password }
            });

            if (!response || !response.token) {
                throw new Error("Login response did not contain a JWT token.");
            }

            if (!["PLATFORM_ADMIN", "TENANT_ADMIN"].includes(response.role)) {
                throw new Error("This panel supports administrator accounts only.");
            }

            state.token = response.token;
            state.username = username;
            state.role = response.role;
            state.organizationId = response.organizationId ?? null;

            sessionStorage.setItem(SESSION_KEY, JSON.stringify({
                token: state.token,
                username: state.username,
                role: state.role,
                organizationId: state.organizationId
            }));

            $("loginForm").reset();
            showDashboard();

        } catch (error) {
            errorElement.textContent = error.message;
            errorElement.classList.remove("hidden");
        } finally {
            button.disabled = false;
            button.textContent = "Sign In →";
        }
    }

    function restoreSession() {
        try {
            const saved = JSON.parse(sessionStorage.getItem(SESSION_KEY));

            if (saved && saved.token &&
                ["PLATFORM_ADMIN", "TENANT_ADMIN"].includes(saved.role)) {

                state.token = saved.token;
                state.username = saved.username;
                state.role = saved.role;
                state.organizationId = saved.organizationId;

                showDashboard();
                return;
            }
        } catch (error) {
            console.error("Could not restore session.");
        }

        sessionStorage.removeItem(SESSION_KEY);
        showLogin();
    }

    function logout() {
        sessionStorage.removeItem(SESSION_KEY);

        state.token = null;
        state.username = null;
        state.role = null;
        state.organizationId = null;
        state.organizations = [];
        state.routes = [];
        state.organization = null;
        state.audits = [];

        showLogin();
        toast("Signed out", "Your browser session has been cleared.");
    }

    function showLogin() {
        $("dashboardView").classList.add("hidden");
        $("loginView").classList.remove("hidden");
    }

    // -------------------------------
    // DASHBOARD INITIALIZATION
    // -------------------------------

    function showDashboard() {
        if (!state.token ||
            !["PLATFORM_ADMIN", "TENANT_ADMIN"].includes(state.role)) {
            logout();
            return;
        }

        $("loginView").classList.add("hidden");
        $("dashboardView").classList.remove("hidden");

        $("sidebarUsername").textContent = state.username;
        $("sidebarRole").textContent = humanRole(state.role);
        $("userInitials").textContent = initials(state.username);
        $("roleBadge").textContent = state.role;
        $("breadcrumbRole").textContent =
            state.role === "PLATFORM_ADMIN" ? "PLATFORM" : "TENANT";

        buildNavigation();
        navigate("overview");
    }

    function buildNavigation() {
        const platformItems = [
            { id: "overview", label: "▦  Overview" },
            { id: "organizations", label: "▣  Organizations" }
        ];

        const tenantItems = [
            { id: "overview", label: "▦  Overview" },
            { id: "routes", label: "⇄  API Routes" },
            { id: "organizationProfile", label: "▤  My Organization" },
            { id: "audit", label: "◷  Audit Records" }
        ];

        const items = state.role === "PLATFORM_ADMIN"
            ? platformItems : tenantItems;

        const navigation = $("mainNavigation");
        navigation.replaceChildren();

        items.forEach(item => {
            const button = document.createElement("button");
            button.type = "button";
            button.dataset.section = item.id;
            button.textContent = item.label;
            button.addEventListener("click", () => navigate(item.id));
            navigation.appendChild(button);
        });
    }

    async function navigate(section) {
        const allowedSections = state.role === "PLATFORM_ADMIN"
            ? ["overview", "organizations"]
            : ["overview", "routes", "organizationProfile", "audit"];

        if (!allowedSections.includes(section)) return;

        state.currentSection = section;

        const sections = {
            overview: $("overviewSection"),
            organizations: $("organizationsSection"),
            routes: $("routesSection"),
            organizationProfile: $("organizationProfileSection"),
            audit: $("auditSection")
        };

        const titles = {
            overview: "Overview",
            organizations: "Organizations",
            routes: "API Routes",
            organizationProfile: "My Organization",
            audit: "Audit Records"
        };

        Object.entries(sections).forEach(([key, element]) => {
            element.classList.toggle("hidden", key !== section);
        });

        document.querySelectorAll("#mainNavigation button").forEach(button => {
            button.classList.toggle("active", button.dataset.section === section);
        });

        $("pageTitle").textContent = titles[section];
        $("sidebar").classList.remove("open");

        try {
            if (section === "overview") {
                await loadOverview();
            } else if (section === "organizations") {
                await loadOrganizations();
            } else if (section === "routes") {
                await loadRoutes();
            } else if (section === "organizationProfile") {
                await loadTenantOrganization();
            } else if (section === "audit") {
                await loadAudits();
            }
        } catch (error) {
            toast("Could not load page", error.message, true);
        }
    }

    // -------------------------------
    // OVERVIEW
    // -------------------------------

    async function loadOverview() {
        $("overviewSection").innerHTML = loadingBlock("Loading dashboard data...");

        if (state.role === "PLATFORM_ADMIN") {
            await loadOrganizations(false);
            renderPlatformOverview();
        } else {
            await Promise.all([
                loadRoutes(false),
                loadTenantOrganization(false),
                loadAudits(false)
            ]);
            renderTenantOverview();
        }
    }

    function renderPlatformOverview() {
        const organizations = state.organizations;

        const active = organizations.filter(o => o.status === "ACTIVE").length;
        const pending = organizations.filter(o => o.status === "PENDING").length;
        const suspended = organizations.filter(o => o.status === "SUSPENDED").length;

        $("overviewSection").innerHTML = `
            <div class="section-heading">
                <div>
                    <p class="eyebrow">PLATFORM OPERATIONS</p>
                    <h1>Control plane overview</h1>
                    <p class="muted">Manage organizations and platform-level access.</p>
                </div>
            </div>

            <div class="stats-grid">
                ${statCard("Total organizations", organizations.length, "REGISTERED TENANTS")}
                ${statCard("Active organizations", active, "ACTIVE TENANTS")}
                ${statCard("Pending / Suspended", pending + suspended,
            pending + " pending · " + suspended + " suspended")}
            </div>

            <div class="spotlight-card">
                <div>
                    <p class="eyebrow">PLATFORM MANAGEMENT</p>
                    <h2>Manage your API gateway ecosystem.</h2>
                    <p>Create organizations, assign tenant administrators
                       and manage organization status from one place.</p>
                </div>
                <button class="primary-button" id="overviewAction">
                    Manage organizations →
                </button>
            </div>
        `;

        $("overviewAction").addEventListener("click", () => navigate("organizations"));
    }

    function renderTenantOverview() {
        const routes = state.routes;
        const active = routes.filter(r => r.active).length;

        const policies = routes.filter(r =>
            r.requestsPerMinute ||
            r.idempotencyEnabled ||
            (r.headerRules && r.headerRules.length)
        ).length;

        const organizationName = state.organization
            ? state.organization.name : "Your organization";

        $("overviewSection").innerHTML = `
            <div class="section-heading">
                <div>
                    <p class="eyebrow">TENANT CONTROL PLANE</p>
                    <h1>${escapeHtml(organizationName)}</h1>
                    <p class="muted">Manage your organization's API gateway configuration.</p>
                </div>
            </div>

            <div class="stats-grid">
                ${statCard("Total routes", routes.length, "CONFIGURED ROUTES")}
                ${statCard("Active routes", active, "AVAILABLE TO GATEWAY")}
                ${statCard("Policy-enabled", policies, state.audits.length + " AUDIT RECORDS")}
            </div>

            <div class="spotlight-card">
                <div>
                    <p class="eyebrow">GATEWAY CONFIGURATION</p>
                    <h2>Control how requests reach your services.</h2>
                    <p>Configure route destinations, allowed methods,
                       rate limits, idempotency and header transformations.</p>
                </div>
                <button class="primary-button" id="overviewAction">
                    Manage routes →
                </button>
            </div>
        `;

        $("overviewAction").addEventListener("click", () => navigate("routes"));
    }

    // -------------------------------
    // PLATFORM ADMIN: ORGANIZATIONS
    // -------------------------------

    async function loadOrganizations() {
        state.organizations = await api("/api/platform/organizations");
        renderOrganizations();
    }

    function renderOrganizations() {
        const query = $("organizationSearch").value.trim().toLowerCase();

        const organizations = state.organizations.filter(org =>
            org.name.toLowerCase().includes(query) ||
            org.slug.toLowerCase().includes(query)
        );

        $("organizationsTableBody").innerHTML = organizations.map(org => `
            <tr>
                <td><strong>${escapeHtml(org.name)}</strong><small>ID ${org.id}</small></td>
                <td><span class="path-code">/${escapeHtml(org.slug)}</span></td>
                <td>
                    <select class="status-select" data-action="status" data-id="${org.id}">
                        ${["PENDING", "ACTIVE", "SUSPENDED"].map(status =>
            `<option value="${status}" ${org.status === status ? "selected" : ""}>
                                ${status}
                            </option>`
        ).join("")}
                    </select>
                </td>
                <td>${formatDate(org.createdAt)}</td>
                <td>
                    <div class="row-actions">
                        <button class="row-action" data-action="admin" data-id="${org.id}">Admin</button>
                        <button class="row-action" data-action="edit" data-id="${org.id}">Edit</button>
                        <button class="row-action danger" data-action="delete" data-id="${org.id}">Delete</button>
                    </div>
                </td>
            </tr>
        `).join("");

        $("organizationsEmpty").classList.toggle("hidden", organizations.length > 0);
        $("organizationsTableBody").closest(".table-scroll")
            .classList.toggle("hidden", organizations.length === 0);
    }

    async function handleOrganizationAction(event) {
        const button = event.target.closest("button[data-action]");
        if (!button) return;

        const organization = findById(state.organizations, button.dataset.id);
        if (!organization) return;

        if (button.dataset.action === "edit") {
            openOrganizationDialog(organization);
        } else if (button.dataset.action === "admin") {
            openTenantAdminDialog(organization);
        } else if (button.dataset.action === "delete") {
            await deleteOrganization(organization);
        }
    }

    async function handleOrganizationStatus(event) {
        const select = event.target.closest('select[data-action="status"]');
        if (!select) return;

        const organization = findById(state.organizations, select.dataset.id);
        const oldStatus = organization.status;

        try {
            await api(`/api/platform/organizations/${organization.id}/status`, {
                method: "PATCH",
                body: { status: select.value }
            });

            organization.status = select.value;
            toast("Status updated", organization.name + " status changed.");
        } catch (error) {
            select.value = oldStatus;
            toast("Could not update status", error.message, true);
        }
    }

    function openOrganizationDialog(organization = null) {
        $("organizationForm").reset();
        $("organizationId").value = organization ? organization.id : "";
        $("organizationName").value = organization ? organization.name : "";
        $("organizationSlug").value = organization ? organization.slug : "";
        $("organizationDialogTitle").textContent =
            organization ? "Edit organization" : "New organization";

        $("organizationDialog").showModal();
    }

    async function saveOrganization(event) {
        event.preventDefault();

        const id = $("organizationId").value;
        const name = $("organizationName").value.trim();
        const slug = $("organizationSlug").value.trim();

        try {
            await api("/api/platform/organizations" + (id ? "/" + id : ""), {
                method: id ? "PUT" : "POST",
                body: { name, slug }
            });

            $("organizationDialog").close();
            await loadOrganizations();
            toast("Organization saved", name + " has been saved.");
        } catch (error) {
            toast("Could not save organization", error.message, true);
        }
    }

    async function deleteOrganization(organization) {
        if (!confirm("Delete organization " + organization.name + "?")) return;

        try {
            await api(`/api/platform/organizations/${organization.id}`, {
                method: "DELETE"
            });

            await loadOrganizations();
            toast("Organization deleted", organization.name + " was deleted.");
        } catch (error) {
            toast("Could not delete organization", error.message, true);
        }
    }

    // -------------------------------
    // PLATFORM ADMIN: TENANT ADMIN
    // -------------------------------

    // Open the creation dialog directly.
    // The GET lookup is skipped until missing-admin responses
    // are consistently returned as HTTP 404.

    function openTenantAdminDialog(organization) {
        $("tenantAdminForm").reset();

        $("tenantAdminOrganizationId").value = organization.id;

        $("tenantAdminOrganization").textContent =
            `${organization.name} · /${organization.slug}`;

        $("tenantAdminDialogTitle").textContent = "Tenant administrator";

        $("tenantAdminUsername").value = "";
        $("tenantAdminPassword").value = "";
        $("tenantAdminEnabled").checked = true;

        $("tenantAdminPassword").required = true;
        $("tenantAdminPassword").placeholder = "Enter a new password";

        $("deleteTenantAdminButton").classList.add("hidden");

        $("saveTenantAdminButton").textContent = "Save administrator";
        $("saveTenantAdminButton").disabled = false;

        $("tenantAdminDialog").showModal();
    }

    // Create the first tenant administrator for an organization.

    async function saveTenantAdmin(event) {
        event.preventDefault();

        const organizationId = $("tenantAdminOrganizationId").value;
        const username = $("tenantAdminUsername").value.trim();
        const password = $("tenantAdminPassword").value;
        const enabled = $("tenantAdminEnabled").checked;

        if (!organizationId || !username || !password) {
            toast(
                "Missing information",
                "Organization, username and password are required.",
                true
            );
            return;
        }

        const submitButton = $("saveTenantAdminButton");

        try {
            submitButton.disabled = true;
            submitButton.textContent = "Creating...";

            const body = {
                username,
                password,
                enabled
            };

            await api(
                `/api/platform/organizations/${encodeURIComponent(organizationId)}/tenant-admin`,
                {
                    method: "POST",
                    body
                }
            );

            $("tenantAdminDialog").close();
            $("tenantAdminForm").reset();

            toast(
                "Administrator created",
                `Tenant administrator "${username}" was created successfully.`
            );

        } catch (error) {
            toast(
                "Could not create administrator",
                error.message,
                true
            );
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = "Save administrator";
        }
    }

    // Retained for future existing-admin management.
    // Hidden during first-time creation because no admin lookup is done yet.

    async function deleteTenantAdmin() {
        const organizationId = $("tenantAdminOrganizationId").value;

        if (!organizationId) return;

        if (!confirm("Are you sure you want to delete this tenant administrator?")) {
            return;
        }

        try {
            await api(
                `/api/platform/organizations/${encodeURIComponent(organizationId)}/tenant-admin`,
                { method: "DELETE" }
            );

            $("tenantAdminDialog").close();

            toast(
                "Administrator deleted",
                "Tenant administrator has been deleted successfully."
            );

        } catch (error) {
            toast(
                "Could not delete administrator",
                error.message,
                true
            );
        }
    }

    // -------------------------------
    // TENANT ADMIN: ROUTES
    // -------------------------------

    async function loadRoutes() {
        state.routes = await api("/api/tenant/routes");
        renderRoutes();
    }

    function renderRoutes() {
        const query = $("routeSearch").value.trim().toLowerCase();

        const routes = state.routes.filter(route =>
            route.routeId.toLowerCase().includes(query) ||
            route.name.toLowerCase().includes(query) ||
            route.pathPattern.toLowerCase().includes(query)
        );

        $("routesTableBody").innerHTML = routes.map(route => {
            const tags = [];

            if (route.requestsPerMinute) tags.push(route.requestsPerMinute + "/min");
            if (route.idempotencyEnabled) tags.push("Idempotent");

            if (route.headerRules && route.headerRules.length) {
                tags.push(route.headerRules.length + " headers");
            }

            if (!tags.length) tags.push("Base routing");

            return `
                <tr>
                    <td>
                        <strong>${escapeHtml(route.name)}</strong>
                        <small>${escapeHtml(route.routeId)} · ${escapeHtml((route.allowedMethods || []).join(", "))}</small>
                    </td>

                    <td>
                        <span class="path-code">${escapeHtml(route.pathPattern)}</span>
                    </td>

                    <td>
                        <div class="policy-tags">
                            ${tags.map(tag =>
                `<span class="policy-tag">${escapeHtml(tag)}</span>`
            ).join("")}
                        </div>
                    </td>

                    <td>
                        <span class="status ${route.active ? "status-active" : "status-inactive"}">
                            ${route.active ? "ACTIVE" : "INACTIVE"}
                        </span>
                    </td>

                    <td>v${route.configurationVersion}</td>

                    <td>
                        <div class="row-actions">
                            <button class="row-action" data-action="versions" data-id="${route.id}">Versions</button>
                            <button class="row-action" data-action="toggle" data-id="${route.id}">
                                ${route.active ? "Deactivate" : "Activate"}
                            </button>
                            <button class="row-action" data-action="edit" data-id="${route.id}">Edit</button>
                            <button class="row-action danger" data-action="delete" data-id="${route.id}">Delete</button>
                        </div>
                    </td>
                </tr>
            `;
        }).join("");

        $("routesEmpty").classList.toggle("hidden", routes.length > 0);

        $("routesTableBody").closest(".table-scroll")
            .classList.toggle("hidden", routes.length === 0);
    }

    async function handleRouteAction(event) {
        const button = event.target.closest("button[data-action]");
        if (!button) return;

        const route = findById(state.routes, button.dataset.id);
        if (!route) return;

        if (button.dataset.action === "edit") {
            openRouteDialog(route);
        } else if (button.dataset.action === "toggle") {
            await toggleRoute(route);
        } else if (button.dataset.action === "delete") {
            await deleteRoute(route);
        } else if (button.dataset.action === "versions") {
            await showVersions(route);
        }
    }

    function openRouteDialog(route = null) {
        $("routeForm").reset();
        $("headerRulesContainer").replaceChildren();

        $("routePk").value = route ? route.id : "";

        $("routeDialogTitle").textContent =
            route ? "Edit route" : "Create route";

        $("routeId").value = route ? route.routeId : "";
        $("routeId").disabled = Boolean(route);

        $("routeName").value = route ? route.name : "";
        $("routePathPattern").value = route ? route.pathPattern : "/";
        $("routeTargetBaseUrl").value = route ? route.targetBaseUrl : "";
        $("routeTargetPathPrefix").value = route ? route.targetPathPrefix : "/";

        $("routeRequestsPerMinute").value =
            route?.requestsPerMinute || "";

        $("routeResponseTimeout").value =
            route ? route.responseTimeoutMs : 3000;

        $("routeAllowedMethods").value =
            route ? route.allowedMethods.join(", ") : "GET";

        $("routeIdempotency").checked =
            route ? route.idempotencyEnabled : false;

        $("routeActive").checked =
            route ? route.active : false;

        if (route && route.headerRules) {
            route.headerRules.forEach(addHeaderRule);
        }

        $("routeDialog").showModal();
    }

    function addHeaderRule(rule = null) {
        const row = document.createElement("div");
        row.className = "rule-row";

        row.innerHTML = `
            <select class="rule-direction">
                <option value="REQUEST">Request</option>
                <option value="RESPONSE">Response</option>
            </select>

            <select class="rule-action">
                <option value="ADD">Add</option>
                <option value="REMOVE">Remove</option>
            </select>

            <input class="rule-name" placeholder="Header name" required>
            <input class="rule-value" placeholder="Header value">

            <button type="button" class="remove-rule">×</button>
        `;

        row.querySelector(".rule-direction").value =
            rule?.direction || "REQUEST";

        row.querySelector(".rule-action").value =
            rule?.action || "ADD";

        row.querySelector(".rule-name").value =
            rule?.headerName || "";

        row.querySelector(".rule-value").value =
            rule?.headerValue || "";

        $("headerRulesContainer").appendChild(row);
    }

    function removeHeaderRule(event) {
        const button = event.target.closest(".remove-rule");

        if (button) {
            button.closest(".rule-row").remove();
        }
    }

    async function saveRoute(event) {
        event.preventDefault();

        const id = $("routePk").value;

        const methods = $("routeAllowedMethods").value
            .split(",")
            .map(method => method.trim().toUpperCase())
            .filter(Boolean);

        const headerRules = [
            ...document.querySelectorAll("#headerRulesContainer .rule-row")
        ].map(row => {
            const action = row.querySelector(".rule-action").value;

            return {
                direction: row.querySelector(".rule-direction").value,
                action,
                headerName: row.querySelector(".rule-name").value.trim(),
                headerValue: action === "REMOVE"
                    ? null
                    : row.querySelector(".rule-value").value.trim()
            };
        });

        const body = {
            routeId: $("routeId").value.trim(),
            name: $("routeName").value.trim(),
            pathPattern: $("routePathPattern").value.trim(),
            targetBaseUrl: $("routeTargetBaseUrl").value.trim(),
            targetPathPrefix: $("routeTargetPathPrefix").value.trim(),

            requestsPerMinute: $("routeRequestsPerMinute").value
                ? Number($("routeRequestsPerMinute").value)
                : null,

            responseTimeoutMs: Number($("routeResponseTimeout").value),
            idempotencyEnabled: $("routeIdempotency").checked,
            active: $("routeActive").checked,
            allowedMethods: methods,
            headerRules
        };

        try {
            await api("/api/tenant/routes" + (id ? "/" + id : ""), {
                method: id ? "PUT" : "POST",
                body
            });

            $("routeDialog").close();

            await loadRoutes();

            toast("Route saved", body.routeId + " has been saved.");

        } catch (error) {
            toast("Could not save route", error.message, true);
        }
    }

    async function toggleRoute(route) {
        try {
            await api(`/api/tenant/routes/${route.id}/status`, {
                method: "PATCH",
                body: { active: !route.active }
            });

            await loadRoutes();

            toast("Route status updated", route.routeId + " was updated.");

        } catch (error) {
            toast("Could not update route", error.message, true);
        }
    }

    async function deleteRoute(route) {
        if (!confirm("Delete route " + route.routeId + "?")) return;

        try {
            await api(`/api/tenant/routes/${route.id}`, {
                method: "DELETE"
            });

            await loadRoutes();

            toast("Route deleted", route.routeId + " was deleted.");

        } catch (error) {
            toast("Could not delete route", error.message, true);
        }
    }

    async function showVersions(route) {
        $("versionsDialogTitle").textContent =
            route.routeId + " versions";

        $("versionsTimeline").innerHTML =
            loadingBlock("Loading version history...");

        $("versionsDialog").showModal();

        try {
            const versions = await api(
                "/api/tenant/route-versions/" +
                encodeURIComponent(route.routeId)
            );

            if (!versions || !versions.length) {
                $("versionsTimeline").innerHTML =
                    '<p class="empty-state">No versions found.</p>';
                return;
            }

            $("versionsTimeline").innerHTML = versions.map(version => `
                <article class="timeline-item">
                    <h3>
                        Version ${version.versionNumber} ·
                        ${escapeHtml(version.changeType)}
                    </h3>

                    <p>${formatDateTime(version.createdAt)}</p>

                    <details>
                        <summary>View JSON snapshot</summary>
                        <pre>${escapeHtml(prettyJson(version.configurationSnapshot))}</pre>
                    </details>
                </article>
            `).join("");

        } catch (error) {
            $("versionsDialog").close();

            toast("Could not load versions", error.message, true);
        }
    }

    // -------------------------------
    // TENANT ADMIN: ORGANIZATION
    // -------------------------------

    async function loadTenantOrganization() {
        state.organization = await api("/api/tenant/organization");
        renderTenantOrganization();
    }

    function renderTenantOrganization() {
        const org = state.organization;

        $("tenantOrganizationCard").innerHTML = `
            <article class="profile-card">
                <p class="eyebrow">TENANT IDENTITY</p>

                <h2>${escapeHtml(org.name)}</h2>

                <p class="muted">/${escapeHtml(org.slug)}</p>

                <div class="profile-details">
                    <div>
                        <span>ORGANIZATION ID</span>
                        <strong>${org.id}</strong>
                    </div>

                    <div>
                        <span>STATUS</span>
                        <strong>${escapeHtml(org.status)}</strong>
                    </div>

                    <div>
                        <span>CREATED</span>
                        <strong>${formatDate(org.createdAt)}</strong>
                    </div>

                    <div>
                        <span>LAST UPDATED</span>
                        <strong>${formatDate(org.updatedAt)}</strong>
                    </div>
                </div>
            </article>

            <form id="tenantOrganizationForm" class="profile-card">
                <h3>Update display name</h3>

                <p class="muted">
                    The URL slug and lifecycle status remain platform-controlled.
                </p>

                <label>Organization name</label>

                <input
                    id="tenantOrganizationName"
                    maxlength="150"
                    value="${escapeAttribute(org.name)}"
                    required
                >

                <button class="primary-button" type="submit">
                    Save name
                </button>
            </form>
        `;

        $("tenantOrganizationForm").addEventListener(
            "submit",
            saveTenantOrganization
        );
    }

    async function saveTenantOrganization(event) {
        event.preventDefault();

        const name = $("tenantOrganizationName").value.trim();

        try {
            await api("/api/tenant/organization", {
                method: "PUT",
                body: { name }
            });

            await loadTenantOrganization();

            toast("Organization updated", name + " was updated.");

        } catch (error) {
            toast("Could not update organization", error.message, true);
        }
    }

    // -------------------------------
    // TENANT ADMIN: AUDIT RECORDS
    // -------------------------------

    async function loadAudits() {
        state.audits = await api("/api/tenant/audit-records");
        renderAudits();
    }

    function renderAudits() {
        $("auditTableBody").innerHTML = state.audits.map(audit => `
            <tr>
                <td>
                    <strong>${escapeHtml(audit.action)}</strong>
                    <small>${audit.durationMs} ms</small>
                </td>

                <td>
                    <strong>${escapeHtml(audit.resourceType)}</strong>
                    <small>${escapeHtml(audit.resourceId)}</small>
                </td>

                <td>${escapeHtml(audit.actor)}</td>

                <td>
                    <span class="status status-active">
                        ${escapeHtml(audit.result)}
                    </span>
                </td>

                <td>${escapeHtml(audit.correlationId || "—")}</td>

                <td>${formatDateTime(audit.createdAt)}</td>
            </tr>
        `).join("");

        $("auditEmpty").classList.toggle(
            "hidden",
            state.audits.length > 0
        );

        $("auditTableBody").closest(".table-scroll")
            .classList.toggle("hidden", state.audits.length === 0);
    }

    // -------------------------------
    // COMMON API REQUEST HANDLER
    // -------------------------------

    async function api(path, options = {}) {
        const method = options.method || "GET";

        const headers = {
            "Accept": "application/json"
        };

        if (options.body !== undefined) {
            headers["Content-Type"] = "application/json";
        }

        if (!options.publicRequest && state.token) {
            headers["Authorization"] = "Bearer " + state.token;
        }

        if (method !== "GET") {
            headers["X-Correlation-ID"] = correlationId();
        }

        const response = await fetch(path, {
            method,
            headers,

            body: options.body === undefined
                ? undefined
                : JSON.stringify(options.body)
        });

        if (response.status === 401 && !options.publicRequest) {
            logout();

            throw new Error(
                "Your session expired. Please sign in again."
            );
        }

        if (response.status === 404 && options.allow404) {
            return null;
        }

        let data = null;

        const contentType =
            response.headers.get("content-type") || "";

        if (contentType.includes("application/json")) {
            data = await response.json();
        }

        if (!response.ok) {
            let message = data?.message ||
                "Request failed with status " + response.status;

            if (data?.errors) {
                message += " — " + Object.entries(data.errors)
                    .map(([key, value]) => key + ": " + value)
                    .join(" · ");
            }

            throw new Error(message);
        }

        return data;
    }

    // -------------------------------
    // UTILITIES
    // -------------------------------

    function statCard(label, value, detail) {
        return `
            <article class="stat-card">
                <span>${escapeHtml(label)}</span>
                <strong>${value}</strong>
                <small>${escapeHtml(detail)}</small>
            </article>
        `;
    }

    function loadingBlock(message) {
        return `
            <div class="empty-state">
                ${escapeHtml(message)}
            </div>
        `;
    }

    function findById(list, id) {
        return list.find(item => String(item.id) === String(id));
    }

    function initials(value) {
        return (value || "GK")
            .split(/[-_.\s]+/)
            .map(part => part.charAt(0))
            .join("")
            .slice(0, 2)
            .toUpperCase();
    }

    function humanRole(role) {
        return role === "PLATFORM_ADMIN"
            ? "Platform Administrator"
            : "Tenant Administrator";
    }

    function formatDate(value) {
        if (!value) return "—";

        return new Intl.DateTimeFormat(undefined, {
            year: "numeric",
            month: "short",
            day: "numeric"
        }).format(new Date(value));
    }

    function formatDateTime(value) {
        if (!value) return "—";

        return new Intl.DateTimeFormat(undefined, {
            year: "numeric",
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        }).format(new Date(value));
    }

    function prettyJson(value) {
        try {
            return JSON.stringify(
                typeof value === "string"
                    ? JSON.parse(value)
                    : value,
                null,
                2
            );
        } catch (error) {
            return String(value ?? "");
        }
    }

    function correlationId() {
        return window.crypto?.randomUUID
            ? window.crypto.randomUUID()
            : "ui-" + Date.now() + "-" +
            Math.random().toString(16).slice(2);
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function escapeAttribute(value) {
        return escapeHtml(value).replace(/`/g, "&#096;");
    }

    function toast(title, message, error = false) {
        const element = document.createElement("div");

        element.className = "toast" + (error ? " error" : "");

        const heading = document.createElement("strong");
        heading.textContent = title;

        const description = document.createElement("span");
        description.textContent = message;

        element.append(heading, description);

        $("toastContainer").appendChild(element);

        setTimeout(() => element.remove(), 4200);
    }

})();