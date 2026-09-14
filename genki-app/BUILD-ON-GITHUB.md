# Building the APK on GitHub (no Android Studio needed)

Do steps 1-4 once. After that, every build is one click.

---

## 1. Put the project on GitHub

1. Sign in at https://github.com and click **New repository**
2. Name it `genki-english-app`
3. Choose **Private** — this matters, the project will contain your
   server key handling and Ruffle's files
4. Create it
5. On the next page click **uploading an existing file**
6. Drag in the whole contents of `D:\genki-app` — including the
   `.github` folder and the `ruffle` files you copied into assets
7. Click **Commit changes**

If the browser upload struggles with the number of files, install
GitHub Desktop (https://desktop.github.com) and drag the folder in
there instead. It handles large batches better.

---

## 2. Create the signing key

This is the file that proves updates come from you. Android refuses to
install an update signed with a different key, so this file must survive
for as long as the app does.

1. Go to the **Actions** tab of your repo
2. Pick **Create keystore (run once)** on the left
3. Click **Run workflow**, type a password you will not forget, run it
4. When it finishes (about a minute), open the run and download the
   artifact **keystore-KEEP-THIS-SAFE**
5. Unzip it. You get `genki.jks` and `genki.jks.base64`
6. **Back up `genki.jks`** — Google Drive, a USB stick, somewhere that
   is not just this laptop. Write the password down with it.

The artifact is deleted from GitHub after one day on purpose. Do not
rely on it being there.

---

## 3. Add four secrets

In your repo: **Settings > Secrets and variables > Actions >
New repository secret**. Add these four:

| Name | Value |
|---|---|
| `KEYSTORE_BASE64` | the entire contents of `genki.jks.base64` |
| `KEYSTORE_PASSWORD` | the password you chose in step 2 |
| `KEY_ALIAS` | `genki` |
| `KEY_PASSWORD` | the same password again |

For `KEYSTORE_BASE64`, open the `.base64` file in Notepad, select all,
copy, paste. It is one very long line with no spaces — that is correct.

---

## 4. Build

1. **Actions** tab > **Build APK** > **Run workflow**
2. Wait about five minutes
3. Open the finished run, download the artifact **genki-english-apk**
4. Inside is your APK

Upload that APK to your website for teachers to download.

---

## Later: making changes

Edit a file on GitHub (or upload a new version) and the build runs by
itself. Download the new APK from Actions when it finishes.

The version number goes up automatically with each build, so boards
will accept it as an update and keep their downloaded games.

---

## If the build fails

Open the failed run and click through to the red step — the error is at
the bottom of the log. Paste it to me and I will tell you what to fix.
The first build is the one most likely to complain; after it succeeds
once it tends to keep succeeding.
