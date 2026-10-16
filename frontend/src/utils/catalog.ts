// Catalog codes look like "HOME_REPAIR__ELECTRICIAN": <CATEGORY>__<SERVICE>.
export function categoryOfCode(code: string): string {
  const i = code.indexOf('__');
  return i === -1 ? code : code.slice(0, i);
}

// Fallback label when the catalog hasn't been loaded: "HOME_REPAIR__TAP_REPAIR" -> "Tap Repair"
export function prettyCode(code: string): string {
  const i = code.indexOf('__');
  const tail = i === -1 ? code : code.slice(i + 2);
  return tail.toLowerCase().split('_').filter(Boolean).map((w) => w[0]!.toUpperCase() + w.slice(1)).join(' ');
}

// Categories where a customer may reasonably want to choose the worker's gender
// (someone entering the home / personal care). Everything else never shows the option.
export const GENDER_RELEVANT_CATEGORIES = new Set([
  'CLEANING', 'COOKING_KITCHEN', 'DOMESTIC', 'CARE_WELLNESS', 'BEAUTY_PERSONAL',
]);

export const isGenderRelevant = (categoryCode: string) => GENDER_RELEVANT_CATEGORIES.has(categoryCode);

export function splitCsv(csv: string | null | undefined): string[] {
  if (!csv) return [];
  return csv.split(',').map((s) => s.trim()).filter(Boolean);
}
