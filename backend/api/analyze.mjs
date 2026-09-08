const MAX_IMAGE_BYTES = 6 * 1024 * 1024;

export const config = {
  api: {
    bodyParser: {
      sizeLimit: "9mb"
    }
  }
};

const shortString = {
  type: "string",
  maxLength: 180
};

const recognitionSchema = {
  type: "object",
  additionalProperties: false,
  properties: {
    itemName: shortString,
    category: shortString,
    brand: shortString,
    modelNumber: shortString,
    serialNumber: shortString,
    locationSuggestion: shortString,
    filterPartNumber: shortString,
    notes: {
      type: "string",
      maxLength: 320
    },
    confidence: {
      type: "string",
      enum: ["high", "medium", "low"]
    },
    recognizedText: {
      type: "string",
      maxLength: 500
    }
  },
  required: [
    "itemName",
    "category",
    "brand",
    "modelNumber",
    "serialNumber",
    "locationSuggestion",
    "filterPartNumber",
    "notes",
    "confidence",
    "recognizedText"
  ]
};

export default async function handler(request, response) {
  if (request.method !== "POST") {
    response.setHeader("Allow", "POST");
    return response.status(405).json({
      error: "Method not allowed."
    });
  }

  if (!process.env.OPENAI_API_KEY) {
    return response.status(503).json({
      error: "Recognition is not configured."
    });
  }

  const imageDataUrl = request.body?.imageDataUrl;

  if (
    typeof imageDataUrl !== "string" ||
    !imageDataUrl.startsWith("data:image/jpeg;base64,")
  ) {
    return response.status(400).json({
      error: "A JPEG image is required."
    });
  }

  const base64 =
    imageDataUrl.substring(
      imageDataUrl.indexOf(",") + 1
    );

  if (
    Buffer.byteLength(
      base64,
      "base64"
    ) > MAX_IMAGE_BYTES
  ) {
    return response.status(413).json({
      error: "Image is too large."
    });
  }

  try {
    const openAiResponse =
      await fetch(
        "https://api.openai.com/v1/responses",
        {
          method: "POST",
          headers: {
            "Authorization":
              `Bearer ${process.env.OPENAI_API_KEY}`,
            "Content-Type":
              "application/json"
          },
          body: JSON.stringify({
            model: "gpt-5.6-terra",
            reasoning: {
              effort: "low"
            },
            input: [
              {
                role: "user",
                content: [
                  {
                    type: "input_text",
                    text:
                      "Analyze this homeowner appliance or household-equipment photo. Extract only information visibly supported by the image. Never guess model, serial, filter, part numbers, dates, warranties, brands, or locations. Use exact visible label text for model and serial fields. Return empty strings for uncertain fields. If there is no useful appliance or readable label, leave uncertain item fields empty and use low confidence. Keep notes concise. recognizedText must contain only the most useful clearly visible text and must stay under 500 characters."
                  },
                  {
                    type: "input_image",
                    image_url:
                      imageDataUrl,
                    detail: "high"
                  }
                ]
              }
            ],
            max_output_tokens: 1800,
            text: {
              format: {
                type: "json_schema",
                name:
                  "housemind_recognition",
                strict: true,
                schema:
                  recognitionSchema
              }
            }
          })
        }
      );

    if (!openAiResponse.ok) {
      const detail =
        await openAiResponse.text();

      console.error(
        "OpenAI recognition request failed",
        openAiResponse.status,
        detail
      );

      return response.status(502).json({
        error:
          "Recognition provider request failed."
      });
    }

    const openAiJson =
      await openAiResponse.json();

    if (
      openAiJson.status === "incomplete"
    ) {
      console.error(
        "Recognition response incomplete",
        JSON.stringify(
          openAiJson.incomplete_details
        )
      );

      return response.status(502).json({
        error:
          "Recognition response was incomplete. Please try the photo again."
      });
    }

    const outputText =
      extractOutputText(
        openAiJson
      );

    if (
      !outputText.trim()
    ) {
      console.error(
        "Recognition response contained no output text",
        JSON.stringify({
          status:
            openAiJson.status,
          error:
            openAiJson.error,
          incomplete_details:
            openAiJson.incomplete_details
        })
      );

      return response.status(502).json({
        error:
          "Recognition returned no readable result."
      });
    }

    let result;

    try {
      result =
        JSON.parse(
          outputText
        );
    } catch (error) {
      console.error(
        "Recognition JSON parse failed",
        error,
        "outputLength=",
        outputText.length,
        "output=",
        outputText
      );

      return response.status(502).json({
        error:
          "Recognition result was incomplete. Please try again."
      });
    }

    if (
      !isValidResult(
        result
      )
    ) {
      console.error(
        "Invalid recognition response",
        JSON.stringify(
          result
        )
      );

      return response.status(502).json({
        error:
          "Recognition returned an invalid result."
      });
    }

    return response
      .status(200)
      .json(result);
  } catch (error) {
    console.error(
      "Recognition failed",
      error
    );

    return response.status(502).json({
      error:
        "Recognition failed."
    });
  }
}

function isValidResult(result) {
  return (
    result &&
    typeof result === "object" &&
    ["high", "medium", "low"]
      .includes(
        result.confidence
      ) &&
    Object.keys(
      recognitionSchema.properties
    )
      .every(
        (key) =>
          typeof result[key] ===
          "string"
      )
  );
}

function extractOutputText(
  responseJson
) {
  if (
    !responseJson ||
    !Array.isArray(
      responseJson.output
    )
  ) {
    return "";
  }

  const pieces = [];

  for (
    const item
    of responseJson.output
  ) {
    if (
      !item ||
      item.type !== "message" ||
      !Array.isArray(
        item.content
      )
    ) {
      continue;
    }

    for (
      const part
      of item.content
    ) {
      if (
        part &&
        part.type ===
          "output_text" &&
        typeof part.text ===
          "string"
      ) {
        pieces.push(
          part.text
        );
      }
    }
  }

  return pieces.join("");
}
