#!/usr/bin/env python3
"""Generates the animated architecture diagrams used by the READMEs.

    python3 docs/assets/build_diagram.py

Writes architecture{,.pt-BR}-{light,dark}.svg next to this script. Standard library only.
The animation is pure CSS (@keyframes inside the SVG): a dot follows each arrow in the order a
check-out request really travels, and prefers-reduced-motion turns it off.
"""

from pathlib import Path

OUT = Path(__file__).resolve().parent
W, H = 880, 620
CYCLE = 9.0  # seconds

THEMES = {
    "light": {
        "bg": "#ffffff", "bgs": "#d0d7de", "text": "#1f2328", "muted": "#59636e",
        "card": "#f6f8fa", "cards": "#d0d7de", "zone": "#eef4ff", "zones": "#b6cdf5",
        "zonet": "#2f5bb7", "edge": "#8c959f", "accent": "#d9480f", "code": "#0550ae",
        "db": "#e8f5ee", "dbs": "#9fd3b4", "dbt": "#1a7f37",
    },
    "dark": {
        "bg": "#0d1117", "bgs": "#30363d", "text": "#e6edf3", "muted": "#9198a1",
        "card": "#161b22", "cards": "#3d444d", "zone": "#131c2e", "zones": "#2d4a7a",
        "zonet": "#8cb4ff", "edge": "#6e7681", "accent": "#fb923c", "code": "#79c0ff",
        "db": "#0f2419", "dbs": "#2e6b45", "dbt": "#56d364",
    },
}

TEXT = {
    "en": {
        "title": "How parking-control handles a check-out",
        "desc": (
            "A client (curl, Swagger UI or any frontend) sends an HTTP request to the REST "
            "controllers, which validate it and report errors as RFC 7807 problem details. "
            "The domain services apply the parking rules, load and update spots and sessions "
            "through Spring Data JPA repositories (optimistic locking and a unique open-plate "
            "key prevent double booking), which persist to PostgreSQL 16 with Flyway "
            "migrations. On check-out the service asks the pure PriceCalculator for the amount "
            "(grace period, first hour, started fractions, daily cap), and the controller "
            "returns JSON to the client. The OpenAPI spec and Swagger UI are generated from the "
            "controllers; tests run the same schema on H2 in PostgreSQL mode."
        ),
        "zone": "Spring Boot 3 · Java 21",
        "client": "client", "client_m": "curl · Swagger UI · any frontend",
        "ctrl": "REST controllers", "ctrl_m": "/api/v1 · Bean Validation · RFC 7807",
        "svc": "domain services", "svc_m": "check-in/out · reservations · availability",
        "repo": "Spring Data JPA", "repo_m": "@Version · UNIQUE open plate · row lock",
        "db": "PostgreSQL 16", "db_m": "Flyway migrations",
        "price": "PriceCalculator", "price_m1": "pure function, 19 unit tests",
        "price_m2": "grace · first hour · fractions · daily cap",
        "api": "OpenAPI 3", "api_m": "Swagger UI at /swagger-ui.html",
        "h2": "tests", "h2_m": "same schema on H2 (PostgreSQL mode)",
        "l_req": "POST /sessions/{id}/checkout", "l_json": "JSON · problem+json",
        "l_sql": "SQL", "l_price": "price the stay", "l_docs": "generated from",
    },
    "pt-BR": {
        "title": "Como o parking-control processa um check-out",
        "desc": (
            "Um cliente (curl, Swagger UI ou qualquer frontend) envia uma requisição HTTP aos "
            "controllers REST, que a validam e devolvem erros no formato RFC 7807. Os serviços "
            "de domínio aplicam as regras do estacionamento, leem e atualizam vagas e sessões "
            "pelos repositórios Spring Data JPA (lock otimista e chave única de placa aberta "
            "impedem reserva dupla), que gravam no PostgreSQL 16 com migrações Flyway. No "
            "check-out o serviço pede o valor à função pura PriceCalculator (tolerância, "
            "primeira hora, frações iniciadas, teto diário) e o controller devolve JSON ao "
            "cliente. A especificação OpenAPI e o Swagger UI são gerados a partir dos "
            "controllers; os testes rodam o mesmo schema no H2 em modo PostgreSQL."
        ),
        "zone": "Spring Boot 3 · Java 21",
        "client": "cliente", "client_m": "curl · Swagger UI · qualquer frontend",
        "ctrl": "controllers REST", "ctrl_m": "/api/v1 · Bean Validation · RFC 7807",
        "svc": "serviços de domínio", "svc_m": "entrada/saída · reservas · disponibilidade",
        "repo": "Spring Data JPA", "repo_m": "@Version · UNIQUE(placa) · lock de linha",
        "db": "PostgreSQL 16", "db_m": "migrações Flyway",
        "price": "PriceCalculator", "price_m1": "função pura, 19 testes unitários",
        "price_m2": "tolerância · 1ª hora · frações · teto diário",
        "api": "OpenAPI 3", "api_m": "Swagger UI em /swagger-ui.html",
        "h2": "testes", "h2_m": "mesmo schema no H2 (modo PostgreSQL)",
        "l_req": "POST /sessions/{id}/checkout", "l_json": "JSON · problem+json",
        "l_sql": "SQL", "l_price": "calcula preço", "l_docs": "gerado de",
    },
}

# Animated edges in the order a check-out travels: (start, end, t0, t1) as fractions of CYCLE.
FLOW = [
    ((120, 76), (120, 144), 0.02, 0.12),  # client -> controllers
    ((120, 224), (120, 272), 0.14, 0.22),  # controllers -> services
    ((120, 352), (120, 400), 0.24, 0.32),  # services -> repositories
    ((120, 480), (120, 540), 0.34, 0.44),  # repositories -> postgres
    ((392, 312), (488, 312), 0.48, 0.58),  # services -> price calculator
    ((488, 330), (392, 330), 0.60, 0.68),  # price -> services (amount)
    ((360, 144), (360, 76), 0.72, 0.84),  # controllers -> client (response)
]


def pct(x: float) -> str:
    return f"{x * 100:.2f}%"


def keyframes(i: int, start, end, t0: float, t1: float) -> str:
    sx, sy = start
    ex, ey = end
    return (
        f"@keyframes m{i}{{"
        f"0%,{pct(t0)}{{transform:translate({sx}px,{sy}px);opacity:0;"
        f"animation-timing-function:ease-in-out}}"
        f"{pct(t0 + 0.015)}{{opacity:1}}"
        f"{pct(t1)}{{transform:translate({ex}px,{ey}px);opacity:1}}"
        f"{pct(t1 + 0.03)},100%{{transform:translate({ex}px,{ey}px);opacity:0}}}}"
        f"@keyframes h{i}{{0%,{pct(max(t0 - 0.01, 0))}{{opacity:0}}"
        f"{pct(t0 + 0.01)},{pct(t1)}{{opacity:.9}}{pct(t1 + 0.07)},100%{{opacity:0}}}}"
        f".m{i}{{animation:m{i} {CYCLE}s linear infinite}}"
        f".h{i}{{animation:h{i} {CYCLE}s linear infinite}}"
    )


def box(x, y, w, h, cls="card", rx=10):
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" class="{cls}"/>'


def label(x, y, text, cls="t", anchor="start"):
    return f'<text x="{x}" y="{y}" class="{cls}" text-anchor="{anchor}">{text}</text>'


def esc(s: str) -> str:
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def render(lang: str, theme: str) -> str:
    c = THEMES[theme]
    t = {k: esc(v) for k, v in TEXT[lang].items()}
    mono = "ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, 'Liberation Mono', monospace"
    sans = "ui-sans-serif, -apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif"
    css = (
        f"text{{font-family:{sans};fill:{c['text']}}}"
        f".t{{font-size:14px}}.b{{font-size:15px;font-weight:600}}"
        f".h{{font-size:12.5px;font-weight:700;letter-spacing:.06em;text-transform:uppercase;"
        f"fill:{c['zonet']}}}"
        f".m{{font-size:12.5px;fill:{c['muted']}}}"
        f".code{{font-family:{mono};font-size:12.5px;fill:{c['code']}}}"
        f".bg{{fill:{c['bg']};stroke:{c['bgs']}}}"
        f".card{{fill:{c['card']};stroke:{c['cards']}}}"
        f".zone{{fill:{c['zone']};stroke:{c['zones']}}}"
        f".db{{fill:{c['db']};stroke:{c['dbs']}}}.dbt{{fill:{c['dbt']}}}"
        f".dash{{fill:none;stroke:{c['cards']};stroke-dasharray:5 4}}"
        f".e{{fill:none;stroke:{c['edge']};stroke-width:1.5}}"
        f".ed{{fill:none;stroke:{c['edge']};stroke-width:1.5;stroke-dasharray:4 4}}"
        f".hl{{fill:none;stroke:{c['accent']};stroke-width:2.5;opacity:0}}"
        f".dot{{fill:{c['accent']}}}.halo{{fill:{c['accent']};opacity:.25}}"
        f".pk{{opacity:0}}"
        + "".join(keyframes(i, *f) for i, f in enumerate(FLOW))
        + "@media (prefers-reduced-motion:reduce){.pk,.hl{animation:none;display:none}}"
    )

    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" '
        f'height="{H}" role="img" aria-labelledby="title desc" lang="{lang}">',
        f'<title id="title">{t["title"]}</title>',
        f'<desc id="desc">{t["desc"]}</desc>',
        f"<style>{css}</style>",
        '<defs><marker id="ah" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" '
        f'markerHeight="7" orient="auto-start-reverse"><path d="M0,1 L9,5 L0,9 z" '
        f'fill="{c["edge"]}"/></marker></defs>',
        f'<rect x="0.5" y="0.5" width="{W - 1}" height="{H - 1}" rx="16" class="bg"/>',
        # client
        box(48, 24, 344, 52, rx=26),
        f'<text x="220" y="55" text-anchor="middle"><tspan class="b">{t["client"]}</tspan>'
        f'<tspan class="m" dx="10">{t["client_m"]}</tspan></text>',
        # application zone
        box(24, 108, 832, 396, "zone", 14),
        label(44, 132, t["zone"], "h"),
        box(48, 144, 344, 80),
        label(68, 176, t["ctrl"], "b"),
        label(68, 202, t["ctrl_m"], "code"),
        box(48, 272, 344, 80),
        label(68, 304, t["svc"], "b"),
        label(68, 330, t["svc_m"], "m"),
        box(48, 400, 344, 80),
        label(68, 432, t["repo"], "b"),
        label(68, 458, t["repo_m"], "code"),
        box(488, 144, 344, 80, "dash"),
        label(508, 176, t["api"], "b"),
        label(508, 202, t["api_m"], "m"),
        box(488, 264, 344, 96),
        label(508, 294, t["price"], "b"),
        label(508, 318, t["price_m1"], "m"),
        label(508, 340, t["price_m2"], "m"),
        # data
        box(48, 540, 344, 60, "db"),
        f'<text x="68" y="575"><tspan class="b dbt">{t["db"]}</tspan>'
        f'<tspan class="m" dx="10">{t["db_m"]}</tspan></text>',
        box(488, 540, 344, 60, "dash"),
        f'<text x="508" y="575"><tspan class="b">{t["h2"]}</tspan>'
        f'<tspan class="m" dx="10">{t["h2_m"]}</tspan></text>',
        # static edges
        '<path d="M120,76 V144" class="e" marker-end="url(#ah)"/>',
        '<path d="M360,144 V76" class="e" marker-end="url(#ah)"/>',
        '<path d="M120,224 V272" class="e" marker-end="url(#ah)"/>',
        '<path d="M120,352 V400" class="e" marker-end="url(#ah)"/>',
        '<path d="M120,480 V540" class="e" marker-start="url(#ah)" marker-end="url(#ah)"/>',
        '<path d="M392,312 H488" class="e" marker-end="url(#ah)"/>',
        '<path d="M488,330 H392" class="e" marker-end="url(#ah)"/>',
        '<path d="M488,184 H392" class="ed" marker-start="url(#ah)"/>',
        # edge labels
        label(372, 98, t["l_json"], "m"),
        label(132, 98, t["l_req"], "code"),
        label(132, 516, t["l_sql"], "m"),
        label(440, 302, t["l_price"], "m", "middle"),
        label(440, 176, t["l_docs"], "m", "middle"),
    ]
    for i, (start, end, _, _) in enumerate(FLOW):
        (sx, sy), (ex, ey) = start, end
        parts.append(f'<path d="M{sx},{sy} L{ex},{ey}" class="hl h{i}"/>')
        parts.append(
            f'<g class="pk m{i}"><circle r="9" class="halo"/><circle r="4.5" class="dot"/></g>'
        )
    parts.append("</svg>")
    return "\n".join(parts) + "\n"


def main() -> None:
    for lang in TEXT:
        suffix = "" if lang == "en" else f".{lang}"
        for theme in THEMES:
            path = OUT / f"architecture{suffix}-{theme}.svg"
            path.write_text(render(lang, theme), encoding="utf-8")
            print(f"wrote {path.relative_to(OUT.parent.parent)}")


if __name__ == "__main__":
    main()
