# Changelog

## Unreleased

- When Hexalia is installed, hide the standalone cauldron recipe category and generate mushroom-free recipes for its four-slot small cauldron.
- Preserve potion data in Hexalia recipe outputs and client synchronization so recipe viewers show the correct effects and forms.

- Move crude splash/lingering creation and upgrades to the cauldron; keep drinkable crude potion processing in brewing stands.
- Increase cauldron capacity to five ingredients, including world rendering and complete lingering recipes.
- Arrange JEI/REI/EMI ingredient slots in an evenly spaced pentagon, whose diagonal-to-side ratio is the golden ratio.
- Add optional native EMI recipe, workstation and potion-variant support.

- Use the shared `c:mushrooms` item tag and `farmersdelight:heat_sources` block tag instead of private brewing tags; Farmer's Delight remains optional.

- Fix crude lingering potions acting as splash potions; their clouds now apply the vanilla duration reduction.
- Match overlapping ingredients independently of insertion order.
- Restore the previous brew when withdrawing an ingredient during further cauldron processing, even after reloading.
- Stop extinguished campfires from heating cauldrons.
- Clear stale brew contents after external water-level changes while preserving remaining bottles during bottling.
- Respect disabled brewing and excluded source potions when processing existing bottles.
- Prevent dilution minimums from strengthening short or low-level source effects; preserve instant effect durations.
- Rebuild the generated data pack when reopened so reloads do not reuse stale generated recipes.
- Add automated NeoForge regression tests to the build.

## 1.0.0

First public release.

- Brew crude potions in a heated vanilla cauldron using mushrooms and ordinary brewing ingredients.
- Discover recipes for compatible vanilla and modded potions at runtime.
- Keep brewing progress and ingredients when the world is saved.
- Process crude potions further in a brewing stand, including splash and lingering forms.
- Configure effect duration, effect strength, excluded namespaces, and cauldron recipe generation.
- Show brewing recipes in JEI and REI, and cauldron contents through Jade.
- Use Hexalia's small cauldron automatically when Hexalia is installed.
- Include English and Simplified Chinese translations.
