export default function handler(request, response) {
  if (request.method !== "GET") {
    response.setHeader("Allow", "GET");
    return response.status(405).send("Method not allowed");
  }

  response.setHeader("Content-Type", "text/html; charset=utf-8");
  response.setHeader("Cache-Control", "public, max-age=300");

  return response.status(200).send(`<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>HouseMind - Delete Your Data</title>
  <style>
    body { margin:0; font-family:Arial,Helvetica,sans-serif; background:#f6f8fc; color:#111827; line-height:1.55; }
    main { max-width:760px; margin:0 auto; padding:48px 20px 64px; }
    .card { background:#fff; border:1px solid #d7dfec; border-radius:20px; padding:28px; box-shadow:0 8px 30px rgba(17,24,39,.06); }
    h1 { margin-top:0; color:#155eef; }
    h2 { margin-top:28px; font-size:1.15rem; }
    ol, ul { padding-left:22px; }
    .note { margin-top:24px; padding:16px; border-radius:14px; background:#edf2fa; }
  </style>
</head>
<body>
  <main>
    <div class="card">
      <h1>HouseMind - Delete Your Data</h1>

      <p>HouseMind currently does not require a user account. Home inventory information, maintenance records, saved appliance details, documents, and locally saved photos are stored on your Android device.</p>

      <h2>Delete all HouseMind data</h2>
      <ol>
        <li>Open Android <strong>Settings</strong>.</li>
        <li>Tap <strong>Apps</strong> and select <strong>HouseMind</strong>.</li>
        <li>Tap <strong>Storage &amp; cache</strong>.</li>
        <li>Tap <strong>Clear storage</strong> or <strong>Clear data</strong>.</li>
      </ol>

      <p>You can also uninstall HouseMind to remove the app and its locally stored data from your device.</p>

      <h2>What is deleted</h2>
      <ul>
        <li>Saved home items and appliance details</li>
        <li>Maintenance tasks and service history</li>
        <li>Saved parts and filter information</li>
        <li>Locally stored documents and photos</li>
        <li>Warranty and replacement-planning information stored by the app</li>
      </ul>

      <div class="note">
        <strong>Cloud account data:</strong> HouseMind does not currently maintain user accounts or a cloud profile that must be separately deleted.
      </div>
    </div>
  </main>
</body>
</html>`);
}
