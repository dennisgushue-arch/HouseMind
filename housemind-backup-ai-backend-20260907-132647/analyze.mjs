const MAX_IMAGE_BYTES = 6 * 1024 * 1024;

export const config = {
  api: {
    bodyParser: {
      sizeLimit: "9mb"
    }
  }
};

const recognitionSchema = {
  type: "object",
  additionalProperties: false,
  properties: {
    itemName: { type: "string" },
    category: { type: "string" },
    brand: { type: "string" },
    modelNumber: { type: "string" },
    serialNumber: { type: "string" },
    locationSuggestion: { type: "string" },
    filterPartNumber: { type: "string" },
    notes: { type: "string" },
    confidence: { type: "string", enum: ["high", "medium", "low"] },
    recognizedText: { type: "string" }
  },
  required: [
    "itemName", "category", "brand", "modelNumber", "serialNumber",
    "locationSuggestion", "filterPartNumber", "notes", "confidence", "recognizedText"
  ]
};

export default async function handler(request, response) {
  if (request.method !== "POST") {
    response.setHeader("Allow", "POST");
    return response.status(405).json({ error: "Method not allowed." });
  }
  if (!process.env.OPENAI_API_KEY) {
    return response.status(503).json({ error: "Recognition is not configured." });
  }

  const imageDataUrl = request.body?.imageDataUrl;
  if (typeof imageDataUrl !== "string" || !imageDataUrl.startsWith("data:image/jpeg;base64,")) {
    return response.status(400).json({ error: "A JPEG image is required." });
  }
  const base64 = imageDataUrl.substring(imageDataUrl.indexOf(",") + 1);
  if (Buffer.byteLength(base64, "base64") > MAX_IMAGE_BYTES) {
    return response.status(413).json({ error: "Image is too large." });
  }

  try {
    const openAiResponse = await fetch("https://api.openai.com/v1/responses", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${process.env.OPENAI_API_KEY}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: "gpt-5.6-terra",
        input: [{
          role: "user",
          content: [
            {
              type: "input_text",
              text: "Analyze this homeowner appliance or household-equipment photo. Extract only information visibly supported by the image. Never guess model, serial, filter, part numbers, dates, warranties, brands, or locations. Use exact visible label text for model and serial fields. Return empty strings for any uncertain field. If there is no useful appliance or readable label, leave item fields empty and use low confidence. recognizedText may contain only text clearly visible in the image."
            },
            { type: "input_image", image_url: imageDataUrl, detail: "high" }
          ]
        }],
        max_output_tokens: 500,
        text: {
          format: {
            type: "json_schema",
            name: "housemind_recognition",
            strict: true,
            schema: recognitionSchema
          }
        }
      })
    });

    if (!openAiResponse.ok) throw new Error("OpenAI request failed");
    const openAiJson = await openAiResponse.json();
    const result = JSON.parse(openAiJson.output_text);
    if (!isValidResult(result)) throw new Error("Invalid recognition response");
    return response.status(200).json(result);
  } catch (error) {
    console.error("Recognition failed", error);
    return response.status(502).json({ error: "Recognition failed." });
  }
}

function isValidResult(result) {
  return result && ["high", "medium", "low"].includes(result.confidence) &&
    Object.keys(recognitionSchema.properties).every((key) => typeof result[key] === "string");
}