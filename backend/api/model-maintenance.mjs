const MAX_TEXT_LENGTH = 120;

const resultSchema = {
  type: "object",
  additionalProperties: false,
  properties: {
    brand: { type: "string" },
    modelNumber: { type: "string" },
    exactModelMatched: { type: "boolean" },
    summary: { type: "string" },
    recommendations: {
      type: "array",
      maxItems: 12,
      items: {
        type: "object",
        additionalProperties: false,
        properties: {
          name: { type: "string" },
          kind: {
            type: "string",
            enum: ["Filter", "Part", "Battery", "Maintenance", "Other"]
          },
          partNumber: { type: "string" },
          maintenanceTitle: { type: "string" },
          hasInterval: { type: "boolean" },
          intervalValue: { type: "integer", minimum: 0, maximum: 3650 },
          intervalUnit: {
            type: "string",
            enum: ["days", "weeks", "months", "years"]
          },
          instructions: { type: "string" },
          sourceTitle: { type: "string" },
          sourceUrl: { type: "string" },
          verification: {
            type: "string",
            enum: ["manufacturer", "secondary"]
          }
        },
        required: [
          "name",
          "kind",
          "partNumber",
          "maintenanceTitle",
          "hasInterval",
          "intervalValue",
          "intervalUnit",
          "instructions",
          "sourceTitle",
          "sourceUrl",
          "verification"
        ]
      }
    }
  },
  required: [
    "brand",
    "modelNumber",
    "exactModelMatched",
    "summary",
    "recommendations"
  ]
};

export default async function handler(request, response) {
  if (request.method !== "POST") {
    response.setHeader("Allow", "POST");
    return response.status(405).json({ error: "Method not allowed." });
  }

  if (!process.env.OPENAI_API_KEY) {
    return response.status(503).json({
      error: "Model research is not configured."
    });
  }

  const brand = clean(request.body?.brand);
  const modelNumber = clean(request.body?.modelNumber);
  const category = clean(request.body?.category);

  if (!brand || !modelNumber) {
    return response.status(400).json({
      error: "Brand and model number are required."
    });
  }

  try {
    const prompt = `
You are the verification engine for HouseMind, a homeowner maintenance application.

Research this exact appliance/home-system model on the live web:

Brand: ${brand}
Model number: ${modelNumber}
Category: ${category || "Unknown"}

GOAL
Find model-specific consumer replacement parts, consumables, filters, batteries, and manufacturer-specified routine maintenance intervals.

STRICT ACCURACY RULES
1. Search the exact model number. Do not infer a part merely because it is common for the brand.
2. Prefer official manufacturer owners manuals, support pages, specifications, parts pages, and manufacturer-hosted PDFs.
3. Only mark verification="manufacturer" when the cited source is an official manufacturer source and supports the exact model/model family.
4. If only a reputable secondary source supports a recommendation, set verification="secondary".
5. Never invent a part number, replacement interval, source URL, maintenance task, or compatibility claim.
6. Do not include failure/repair components such as compressors, circuit boards, motors, valves, sensors, or sealed-system parts unless the manufacturer explicitly calls them routine homeowner replacement items.
7. Only set hasInterval=true when the source actually supports a recurring replacement/maintenance interval. Otherwise use hasInterval=false, intervalValue=0, and intervalUnit="months".
8. sourceUrl must be a real URL found during this web research, not a guessed URL.
9. exactModelMatched=true only when the research found evidence for this exact model or an explicitly documented model family that includes it.
10. If exact-model evidence is weak, return an empty recommendations array rather than guessing.

For refrigerator models, specifically look for water filters, air/deodorizing filters, ice-maker filters if applicable, and manufacturer-defined periodic cleaning/maintenance. Do not assume every refrigerator has each item.

Keep instructions concise and homeowner-safe.
`.trim();

    const openAiResponse = await fetch(
      "https://api.openai.com/v1/responses",
      {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${process.env.OPENAI_API_KEY}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          model: "gpt-5.6-terra",
          reasoning: { effort: "low" },
          tools: [
            {
              type: "web_search",
              search_context_size: "medium"
            }
          ],
          input: prompt,
          max_output_tokens: 2200,
          text: {
            format: {
              type: "json_schema",
              name: "housemind_model_maintenance",
              strict: true,
              schema: resultSchema
            }
          }
        })
      }
    );

    if (!openAiResponse.ok) {
      const detail = await openAiResponse.text();
      console.error("OpenAI model research failed", detail);
      return response.status(502).json({
        error: "Model research failed."
      });
    }

    const openAiJson = await openAiResponse.json();
    const outputText = extractOutputText(openAiJson);

    if (typeof outputText !== "string" || !outputText.trim()) {
      throw new Error("No structured model research output");
    }

    const result = JSON.parse(outputText);

    if (!isValidResult(result)) {
      throw new Error("Invalid model research result");
    }

    result.brand = brand;
    result.modelNumber = modelNumber;

    return response.status(200).json(result);
  } catch (error) {
    console.error("HouseMind model research failed", error);

    return response.status(502).json({
      error: "HouseMind could not research that model right now."
    });
  }
}

function clean(value) {
  if (typeof value !== "string") return "";

  return value
    .trim()
    .slice(0, MAX_TEXT_LENGTH);
}

function isValidResult(result) {
  if (
    !result ||
    typeof result !== "object" ||
    typeof result.exactModelMatched !== "boolean" ||
    typeof result.summary !== "string" ||
    !Array.isArray(result.recommendations)
  ) {
    return false;
  }

  return result.recommendations.every((item) => {
    const validUrl =
      item.sourceUrl === "" ||
      /^https:\/\//i.test(item.sourceUrl);

    const validInterval =
      Number.isInteger(item.intervalValue) &&
      item.intervalValue >= 0;

    return (
      typeof item.name === "string" &&
      typeof item.kind === "string" &&
      typeof item.partNumber === "string" &&
      typeof item.maintenanceTitle === "string" &&
      typeof item.hasInterval === "boolean" &&
      validInterval &&
      typeof item.intervalUnit === "string" &&
      typeof item.instructions === "string" &&
      typeof item.sourceTitle === "string" &&
      typeof item.sourceUrl === "string" &&
      validUrl &&
      ["manufacturer", "secondary"].includes(item.verification)
    );
  });
}

function extractOutputText(responseJson) {
  if (!responseJson || !Array.isArray(responseJson.output)) {
    return "";
  }

  const pieces = [];

  for (const item of responseJson.output) {
    if (!item || item.type !== "message" || !Array.isArray(item.content)) {
      continue;
    }

    for (const part of item.content) {
      if (
        part &&
        part.type === "output_text" &&
        typeof part.text === "string"
      ) {
        pieces.push(part.text);
      }
    }
  }

  return pieces.join("");
}

