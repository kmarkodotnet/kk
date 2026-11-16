# Az intro rajzainak forrása – 2026-11-14

Eszköz: a beépített `image_gen` képgeneráló, az `imagegen` készség szerint.
CLI/API-kulcsos generálás nem történt. Az eredeti generált PNG-k alfacsatornája
megmaradt; az offline HTML-be csomagolás WebP-tömörítést használ.

## Ököl

Felhasználói referencia: [fist.jpg](../asserts/fist.jpg).
Végleges projektfájl: [intro-fist-reference.png](../asserts/intro-fist-reference.png).
A korábbi, referencia nélkül generált `intro-fist-realistic.png` nem a használt kép.

A generálás végleges promptja (eredeti nyelven):

> Use the attached fist.jpg as the STRICT pose and thumb-placement reference. Create a production animation sprite of this SAME left-facing clenched fist and right-extending wrist/forearm, but illustrated much more realistically in detailed indigo-blue ink and blue graphite crosshatching with restrained pale blue watercolor shading. Preserve the reference's palm-side orientation, compact curled fingers stacked vertically at left, thumb emerging across the TOP of fist from right then flexing down-left over the outside of the curled index finger, with its nail visible near the upper-left. The thumb must follow the reference exactly; don't relocate it to the lower palm or make an extra finger. Four curled fingers only, natural anatomical joints, realistic proportional hand, straight wrist. Refine form and anatomy from the supplied pose, not cartoon/vector-like black contours. Landscape sprite, full fist visible at left, wrist and a forearm extending to right edge, no sleeve, no other objects, no text, no background, no shadow outside silhouette. Genuine transparent alpha background. Blue drawing, white/light blue interior highlights, fine detailed drawn skin folds and tendon shading. Leave small transparent margin at top bottom and left; forearm exits right.

## Fametszet

Végleges projektfájl: [intro-wood-cross-section.png](../asserts/intro-wood-cross-section.png).

A generálás végleges promptja (eredeti nyelven):

> Single isolated realistic hand-drawn wood animation asset, transparent alpha background. A thick cross section cut from a natural brown oak tree trunk, horizontal oval front-facing cut surface roughly 1.6 times wider than tall, slight perspective showing a thick rough dark brown bark rim below and around sides. Entire log slice visible, center large unobstructed pale warm tan heartwood with extremely fine irregular growth rings, radial wood fibers, subtle variations in ochre browns, tiny natural pores and irregular organically shaped perimeter. Sophisticated natural history illustration in sepia ink, colored pencil and restrained watercolor wash, detailed, realistic yet unmistakably hand drawn. The cut surface clean and intact: no major cracks, no split, no writing, no symbols, no blue, no glowing light. No scenery, no ground, no drop shadow outside the object, no white border, genuine transparent background. A broad smooth cut face fills most of the asset, rough bark rim is thin about 5 percent width, with organic flaking texture. Landscape composition with a small transparent margin on all sides. This will be animated as wood being split from its right edge toward its left side.

Mindkét rajz dekoratív animációs elem, nem hitelesített karateoktató ábra.

## Deszka – a fametszet helyett használt végleges elem

Végleges projektfájl: [intro-wood-board.png](../asserts/intro-wood-board.png).
Beépített `image_gen`, átlátszó háttér. A korábbi fametszet megmaradt, de az
animáció már a deszkát használja. A végleges prompt (eredeti nyelven):

> Single isolated horizontal rectangular wooden plank for an animation, a realistic hand-drawn natural-history illustration in sepia ink, colored pencil and subtle brown watercolor, not a photograph. Rectangular board about twice as wide as tall, flat broad FRONT face viewed almost straight on, slight depth visible along bottom and right edges. Warm medium honey-brown wood with long fine natural grain running horizontally from right to left, subtle irregular fibers and two small unobtrusive knots. Four straight cut sides, lightly worn corners, a real sawn plank, NOT a log or oval slice, NO bark, NO concentric circular rings. The center is intact, no splits, no cracks, no lettering, no symbol, no blue, no other objects. Fine realistic wood texture but unmistakably drawn, softly shaded thickness 5 percent of face height. Entire rectangle visible with small transparent margin on every side, genuine transparent alpha background, no cast shadow outside object. Landscape composition. It will be punched at its right edge and split leftward along the grain.
