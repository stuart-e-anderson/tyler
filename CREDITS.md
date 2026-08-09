# Credits

**Tyler** was created by **Melinda Green** and **Don Hatch** (Superliminal Software),
2002–2003. Original project and gallery: <https://superliminal.com/geometry/tyler/>

The drawing engine — the tiling construction, the three geometries (Euclidean,
hyperbolic, spherical), edge matching, and rendering — is the original authors'
work and is essentially unchanged here.

## Additions in this repository

- PostScript / EPS export (Euclidean, spherical, and arc-accurate hyperbolic)
- Per-tile colour, with an OS-independent recolour tool
- Euclidean rhombus tiles (corner angle a rational multiple of π)
- A modern Swing user interface (`TylerSwing`) with optional FlatLaf theming and
  OS light/dark auto-detect
- A fix so "Save As…" writes to the chosen directory

These additions are offered back to the original project.

## Third-party

- **FlatLaf** (optional, used by the modern UI) © FormDev Software GmbH,
  Apache License 2.0 — <https://github.com/JFormDesigner/FlatLaf>
