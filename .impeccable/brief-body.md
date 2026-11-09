# site/index.html — libra docs

Scope: the single-page documentation site in `site/`, hosted on GitHub Pages. Mode: Read.
Reader: a service engineer integrating with `/v1/evaluate` and writing policies. First question: what is libra and why.
Constraints: lowercase voice, one page with anchors, no javascript. Content comes from README.md, docs/libra.md and the api dtos; no invented claims.

## Direction contract

THESIS: libra documented as an RFC, the form engineers already trust for protocol truth: abstract first, numbered sections, normative MUST/SHOULD for the policy contract, security considerations. It refuses the category default of a sidebar-tree product-docs template with tabbed snippets.

OWN-WORLD: paper white ground, near-black ink, one link blue; allow green and deny red appear only on the verdict words, which are always bracketed so state reads without color. A reading serif for prose; a mono only for the header block, section numbers, code, figures, and tables of fields. Hairline rules, no cards, no shadows.

STORY: the reader meets the abstract and understands libra in four sentences, sees figure 1 (the evaluate exchange as an ascii sequence diagram), then follows numbered sections to the contract, quickstart and api. They leave able to write a policy and call evaluate.

FIRST VIEWPORT: an rfc header block across the top (left: project, document, category; right: status, date), then the title "libra: a multi-tenant policy evaluation engine" centered in the rfc manner, then the abstract in the reading column; on desktop a sticky table of contents rail on the left with hanging section numbers and dot leaders. The primary way forward is the toc and §1.

FORM: request for comments (ietf rfc document), my top-ranked grounded candidate (1 of 7), taken as the pick; seed key fc91b892. Signature move: section numbers hung in the left margin of the column and cited as §n cross-references; normative keywords set in small caps.

ADAPTATIONS (approved or reasoned, after the first review):
- dark ground: the user asked for a light/dark toggle, so a dark ink palette ships beside paper white, chosen by a css-only `theme: auto / light / dark` field in the header block. no javascript means the choice does not persist across loads.
- toc dot leaders: dropped on purpose. a web page has no page numbers, so leaders would lead nowhere; the toc keeps hanging mono numbers instead.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
