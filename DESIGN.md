---
name: libra
description: libra's docs set as an ietf request for comments; paper, ink, one link blue, hairline rules.
colors:
  paper: "#ffffff"
  ink: "#16171a"
  ink-2: "#4a4d55"
  ink-3: "#686c76"
  rule: "#d9dbe0"
  rule-strong: "#16171a"
  link: "#0b57d0"
  allow: "#146c43"
  deny: "#b42318"
  select: "#cfe0ff"
  paper-dark: "#121316"
  ink-dark: "#e8e9ec"
  ink-2-dark: "#b4b7bf"
  ink-3-dark: "#8d919b"
  rule-dark: "#2c2e34"
  rule-strong-dark: "#e8e9ec"
  link-dark: "#8ab4ff"
  allow-dark: "#5fd49a"
  deny-dark: "#ff8a7a"
  select-dark: "#23395f"
typography:
  display:
    fontFamily: "Source Serif 4, Iowan Old Style, Charter, Georgia, serif"
    fontSize: "clamp(1.75rem, 1.2rem + 1.9vw, 2.6rem)"
    fontWeight: 600
    lineHeight: 1.15
    letterSpacing: "-0.015em"
  headline:
    fontFamily: "Source Serif 4, Iowan Old Style, Charter, Georgia, serif"
    fontSize: "1.5rem"
    fontWeight: 600
    lineHeight: 1.25
    letterSpacing: "-0.01em"
  title:
    fontFamily: "Source Serif 4, Iowan Old Style, Charter, Georgia, serif"
    fontSize: "1.1875rem"
    fontWeight: 600
    lineHeight: 1.3
  body:
    fontFamily: "Source Serif 4, Iowan Old Style, Charter, Georgia, serif"
    fontSize: "1.0625rem"
    fontWeight: 400
    lineHeight: 1.6
  body-small:
    fontFamily: "Source Serif 4, Iowan Old Style, Charter, Georgia, serif"
    fontSize: "0.9375rem"
    fontWeight: 400
    lineHeight: 1.45
  keyword:
    fontFamily: "Source Serif 4, Iowan Old Style, Charter, Georgia, serif"
    fontWeight: 600
    letterSpacing: "0.04em"
    fontFeature: "\"smcp\", \"c2sc\""
  label:
    fontFamily: "JetBrains Mono, ui-monospace, SF Mono, Menlo, monospace"
    fontSize: "0.8125rem"
    fontWeight: 400
    lineHeight: 1.7
  number:
    fontFamily: "JetBrains Mono, ui-monospace, SF Mono, Menlo, monospace"
    fontSize: "0.875rem"
    fontWeight: 500
    fontFeature: "\"tnum\""
  code:
    fontFamily: "JetBrains Mono, ui-monospace, SF Mono, Menlo, monospace"
    fontSize: "0.8125rem"
    fontWeight: 400
    lineHeight: 1.6
    fontFeature: "\"liga\" 0, \"calt\" 0"
rounded:
  focus: "2px"
spacing:
  measure: "68ch"
  hang: "4.25rem"
  rail: "15.5rem"
  gutter: "3.5rem"
  paragraph: "1rem"
  section: "4.5rem"
components:
  header-block:
    textColor: "{colors.ink-2}"
    typography: "{typography.label}"
    padding: "2.25rem 0 1.25rem"
  section-heading:
    textColor: "{colors.ink}"
    typography: "{typography.headline}"
    padding: "1.25rem 0 0"
  section-number:
    textColor: "{colors.ink-3}"
    typography: "{typography.number}"
    width: "3.25rem"
  section-number-hover:
    textColor: "{colors.link}"
  toc-link:
    textColor: "{colors.ink-2}"
    typography: "{typography.body-small}"
    padding: "0.2rem 0"
  toc-link-hover:
    textColor: "{colors.link}"
  verdict-allow:
    textColor: "{colors.allow}"
    typography: "{typography.code}"
  verdict-deny:
    textColor: "{colors.deny}"
    typography: "{typography.code}"
  code-block:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    typography: "{typography.code}"
    padding: "1rem 1.25rem"
  theme-switch-option:
    textColor: "{colors.ink-3}"
    typography: "{typography.label}"
    rounded: "{rounded.focus}"
    height: "24px"
  theme-switch-option-checked:
    textColor: "{colors.ink}"
---

# Design System: libra

## Overview

**Creative North Star: "The Request for Comments"**

libra's documentation is set as an ietf rfc: the form engineers already trust for protocol truth. A mono header block of fields opens the page, the title sits centered beneath it, the abstract comes first, and everything after is a numbered section whose number hangs in the left margin and doubles as its anchor. Prose cites prose by `§n`. Normative keywords are set in small capitals under a bcp 14 conventions paragraph. Diagrams are ascii, captioned `figure n:`; tables are captioned `table n:`. The document closes with an author's address, the way an rfc ends.

The page is quiet, dense and typographic. Paper and ink carry everything; one link blue marks what can be followed; green and red exist only for the bracketed verdict words. Structure is drawn with 1px hairlines and whitespace, never with containers, fills or shadow. The serif reads; the mono indexes. A dark ink set ships beside paper white, chosen by a css-only `theme: auto / light / dark` field in the header block; it is the same document on a dark ground, not a second world.

Durable product commitments bind the visual system: lowercase voice throughout (headings, captions, toc), one page with anchors, and no javascript, so every interactive state (theme, focus, hover) is pure css.

**Key Characteristics:**
- Paper ground, near-black ink, one link blue; verdict green and red only inside brackets.
- Source Serif 4 for prose, headings and toc titles; JetBrains Mono only for header fields, numbers, code, figures, field-table heads and the colophon.
- Section numbers hang in the left margin and are the anchors.
- Hairline rules for all structure; no cards, no shadows, no fills on code.
- Light and dark token sets, switched without javascript.

## Colors

A near-monochrome paper-and-ink palette with one functional blue and a verdict pair that never appears outside its brackets. Every role has a light value and a dark value (`-dark` keys); components reference the role, and the theme swaps the set.

### Primary
- **Reference Blue** (`link`, dark `link-dark`): every followable thing: inline links, `§n` cross-references, hovered toc entries and hovered section numbers, the focus outline, the text caret. Visited links keep the same blue; a reference document has no read/unread state.

### Tertiary
- **Allow Green** (`allow`, dark `allow-dark`): only the mono word `ALLOW` inside its brackets.
- **Deny Red** (`deny`, dark `deny-dark`): only the mono word `DENY` inside its brackets.

### Neutral
- **Paper** (`paper`, dark `paper-dark`): the page ground; also the ground of code blocks, which take no fill.
- **Ink** (`ink`, dark `ink-dark`): body text, headings, header-field values, the checked theme option, caption labels (`figure 1:`, `table 1:`).
- **Second Ink** (`ink-2`, dark `ink-2-dark`): the header block and colophon text, toc titles at rest, subtitles, captions, field-table heads.
- **Third Ink** (`ink-3`, dark `ink-3-dark`): margin numbers, toc numbers, header-field names, list markers, step counters, code comments, verdict brackets, unchecked theme options.
- **Hairline** (`rule`, dark `rule-dark`): section top rules, table row rules, code and figure borders, scrollbar thumb.
- **Strong Rule** (`rule-strong`, dark `rule-strong-dark`): ink-weight 1px rules that frame the document: under the header block, above the colophon, and above and below table heads and the last table row.
- **Selection** (`select`, dark `select-dark`): text-selection ground only.

### Named Rules
**The One Blue Rule.** Blue means "you can follow this", and nothing else is blue. No blue headings, rules, fills or decorative accents.

**The Bracketed Verdict Rule.** Green and red appear only on the words `ALLOW` and `DENY`, always set in mono and wrapped in third-ink brackets (`[ALLOW]`, `[DENY]`), so the verdict reads without colour. Never colour a row, border, background or icon by verdict.

## Typography

**Body Font:** Source Serif 4 (variable 200–900, roman and italic, self-hosted; falls back to Iowan Old Style, Charter, Georgia, serif)
**Label/Mono Font:** JetBrains Mono (variable 100–800, self-hosted; falls back to ui-monospace, SF Mono, Menlo, monospace)

**Character:** A reading serif with optical sizing carries the whole argument; a technical mono is reserved for the parts of an rfc that index, quote or tabulate. The split is functional: if it is prose or a title, it is serif.

### Hierarchy
- **Display** (600, clamp 1.75–2.6rem, 1.15): the document title only, centered over the reading column, balanced; its subtitle is the serif italic at 0.55em in second ink.
- **Headline** (600, 1.5rem, 1.25): numbered top-level sections, each preceded by a hairline rule.
- **Title** (600, 1.1875rem, 1.3): numbered subsections. Front-matter headings (abstract, status of this memo, conventions) are 600 at 1.125rem with no number.
- **Body** (400, 1.0625rem, 1.6; 1rem under 640px): all prose, held to a 68ch measure. The abstract's first paragraph steps up to 1.1875rem / 1.55.
- **Body Small** (400, 0.9375rem, 1.45): toc titles, table cells, figure and table captions.
- **Keyword** (600, all-small-caps, 0.04em tracking): bcp 14 words (must, must not, should, may) where they are normative.
- **Label** (mono 400, 0.8125rem, 1.7): the header block and the colophon.
- **Number** (mono 500, tabular figures; 0.875rem on sections, 0.8125rem on subsections and toc): section numbers, toc numbers, step counters, http methods and status codes in tables.
- **Code** (mono 400, 0.8125rem / 1.6 in blocks, 0.84em inline; ligatures off): code, figures, field-table heads (600).

### Named Rules
**The Serif Reads, Mono Indexes Rule.** Mono is allowed only on header fields, numbers, code, figures, field-table heads, verdict words and the colophon. Prose, headings and toc titles are always serif.

**The Small Capitals Rule.** Normative keywords are set in all-small-caps semibold, never uppercase, bold-only or coloured; small capitals are what the conventions paragraph defines as normative.

**The Lowercase Voice Rule.** Headings, captions, labels and toc entries are lowercase; only code identifiers, http methods and verdict words keep their own case.

## Layout

A two-column document on desktop: a 15.5rem table-of-contents rail on the left, a 3.5rem gutter, then the reading column. The header block spans both columns. The reading column has a 4.25rem left margin (the hang) that holds section numbers, then prose at a 68ch measure. The whole grid is centered with 2rem side padding and 6rem bottom padding.

The toc rail is sticky to the top of the viewport, scrolls on its own when taller than it, and indents subsection lists by 2.75ch with wider number columns.

Rhythm: 1rem after paragraphs and lists, 0.35rem between list items; top-level sections open 4.5rem down (3.5rem on small screens) with 1.25rem between rule and heading; subsections 2.75rem; code blocks 1.25rem above and 1.5rem below; figures 2rem; the colophon 5rem.

Responsive behavior:
- **At 1080px and below**, the grid collapses to one column in the order header, front matter, toc, body; the toc becomes static and aligns to the hang.
- **At 640px and below**, the hang becomes 0 and section numbers move inline before their heading; the header block stacks; code blocks and figures bleed to the screen edge with side borders removed, figures shrinking to 0.6875rem mono; api tables reflow into a two-column grid of method and path with notes beneath, header row hidden.

### Named Rules
**The Hang Rule.** Section numbers live in the left margin, outside the measure, right-aligned against the prose. The prose edge never moves to make room for a number.

## Elevation & Depth

The system is flat. There are no shadows, no tonal layers, and no raised surfaces: every element sits on paper. Hierarchy comes from type weight and size, the hang, whitespace, and two weights of 1px rule (hairline and strong). The only stacking is the skip link, which drops in over the page on focus.

### Named Rules
**The Paper Rule.** Nothing floats. No box-shadow, no card, no background fill to set content apart; if a region needs separation, give it a 1px rule or more space.

## Shapes

Square corners throughout. Code blocks, figures and tables are bounded by 1px rules with no radius. The only rounding is a 2px corner on focus outlines and on theme-switch options, where it softens the keyboard ring rather than shaping a surface. Underlines are 1px with a 0.18em offset on links (2px on hover) and 0.25em on the checked theme option.

## Components

### Header Block
The rfc's top matter, set in mono. Two definition lists (left: project, document, category; right: status, date, source, theme) push to either edge over a strong rule. Field names are third ink with a generated colon; values are ink. Links inside inherit the field colour. Under 640px the lists stack and both align left.

### Theme Switch
A header field, not a control panel: `theme: auto / light / dark` as three visually hidden radios with mono labels separated by third-ink slashes. Options are third ink at rest, ink on hover, and ink with a 1px underline when checked. Each label is at least 24px tall. Focus draws the 2px blue outline on the label. Color transitions at 120ms ease-out, removed under reduced motion. Auto follows `prefers-color-scheme`; the choice does not persist across loads.

### Navigation (Table of Contents)
A sticky rail titled "table of contents" in serif 600. Each entry is a flex row: a mono tabular number in third ink, then the serif title in second ink, no underline. Hover turns the whole entry blue and underlines only the title. No dot leaders: the page has no page numbers to lead to.

### Section Heading
A serif heading whose number is a separate mono link absolutely positioned in the hang, right-aligned, third ink; hover turns it blue and underlined. Top-level headings carry a hairline rule above. The number is the anchor; there is no separate permalink glyph.

### Cross-reference
An inline blue link reading `§n` or `§n.m`, never broken across lines.

### Verdict
`ALLOW` or `DENY` in mono 600 at 0.82em, coloured allow or deny, wrapped in regular-weight third-ink brackets generated by css.

### Code Block
Mono 0.8125rem / 1.6 inside a hairline border, 1rem by 1.25rem padding, paper ground, scrolling horizontally. Comments are third ink; there is no other syntax colouring.

### Figure
An ascii diagram in a hairline-bordered block shrink-wrapped and centered, captioned below and centered in body-small second ink, with the label (`figure 1:`) in ink 600.

### Field Table
Full-width, collapsed, tabular figures. Head cells are mono 600 second ink between two strong rules; body rows are separated by hairlines, the last closed by a strong rule. Cells pad 1.25rem on the right only, so columns align to the prose edge. Captions sit below, labelled `table n:`. Method and status cells are mono 500.

### Algorithm Steps
An ordered list with css counters: a mono third-ink `n.` in a 2rem column, prose indented 2.75rem.

### Colophon
The closing "author's address": a strong rule, then mono label-size text in second ink with an ink heading.

## Do's and Don'ts

### Do:
- **Do** hang every section number in the 4.25rem left margin as its own anchor link, and cite sections as `§n`.
- **Do** caption every figure and table below it, centered for figures, with an ink 600 label (`figure n:`, `table n:`).
- **Do** draw structure with 1px rules only: hairline for sections and rows, strong (ink-weight) for the header block, colophon and table heads.
- **Do** define every colour as a role with both a light and a dark value, and reference roles, never literal hex, in components.
- **Do** keep every interactive state pure css: the theme switch, hover, focus and skip link all work with javascript off.
- **Do** hold prose to the 68ch measure and keep the hang outside it.

### Don't:
- **Don't** use colour for anything but links (blue) and bracketed verdict words (green, red).
- **Don't** add cards, shadows, background fills on code, or rounded containers.
- **Don't** set prose, headings or toc titles in mono, or header fields and numbers in serif.
- **Don't** set bcp 14 keywords in uppercase or colour; they are small capitals.
- **Don't** add javascript, including to persist the theme choice.
