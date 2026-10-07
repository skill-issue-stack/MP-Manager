package io.github.abdurazaaqmohammed.adapters.main;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import io.github.abdurazaaqmohammed.ui.UiFields;
import io.github.codehasan.colorpicker.extensions.Extensions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.apk.axml.APKParser;
import com.apk.axml.aXMLDecoder;
import com.apk.axml.aXMLEncoder;
import com.apk.axml.serializableItems.ResEntry;
import com.apk.axml.serializableItems.XMLEntry;
import io.github.abdurazaaqmohammed.ui.dialogs.FilePickerDialog;

import android.os.Environment;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.reandroid.apk.APKLogger;
import com.reandroid.apkeditor.compile.BuildOptions;
import com.reandroid.apkeditor.compile.Builder;
import com.reandroid.apkeditor.decompile.DecompileOptions;
import com.reandroid.apkeditor.decompile.Decompiler;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionMethod;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.abdurazaaqmohammed.MPManager.MainActivity;
import io.github.abdurazaaqmohammed.MPManager.R;
import io.github.abdurazaaqmohammed.ui.UIHelper;
import io.github.abdurazaaqmohammed.utils.ColorUtil;
import io.github.abdurazaaqmohammed.utils.DialogUtil;
import io.github.abdurazaaqmohammed.utils.ErrorUtil;
import io.github.abdurazaaqmohammed.utils.FileUtils;
import io.github.abdurazaaqmohammed.utils.ProgressManager;
import io.github.abdurazaaqmohammed.utils.RunUtil;
import io.github.abdurazaaqmohammed.utils.SignWrapper;
import io.github.abdurazaaqmohammed.utils.UiPrefs;

public class ApkManifestEditor {

    private final MainActivity context;
    private final DialogUtil dialogUtil;
    private final UIHelper uiHelper;
    final Resources rss;

    public ApkManifestEditor(MainActivity context, DialogUtil dialogUtil, UIHelper uiHelper) {
        this.context = context;
        this.dialogUtil = dialogUtil;
        this.uiHelper = uiHelper;
        rss = context.rss;
    }

    private static String shortPermName(String perm) {
        if (perm == null) return "";
        String p = perm.trim();
        int idx = p.lastIndexOf('.');
        return idx >= 0 ? p.substring(idx + 1) : p;
    }

    public void showEditManifestDialog(File apkFile) {
        View quickEditDialog = LayoutInflater.from(context).inflate(R.layout.quick_edit_dialog, null, false);
        quickEditDialog.findViewById(R.id.app_lancer_icon).setOnClickListener(v -> editLauncherIcon(apkFile));

        quickEditDialog.findViewById(R.id.install_location_dropdown);
        AutoCompleteTextView installLocationTextView = quickEditDialog.findViewById(R.id.install_location);
        List<XMLEntry> entries = decodeManifest(apkFile);

        String installLoc = "";
        TextView appNameInput = quickEditDialog.findViewById(R.id.appNameInput);
        TextView verCodeInput = quickEditDialog.findViewById(R.id.verCodeInput);
        TextView verNameInput = quickEditDialog.findViewById(R.id.verNameInput);
        AutoCompleteTextView targetSdk = quickEditDialog.findViewById(R.id.targetSdk);
        AutoCompleteTextView minSdk = quickEditDialog.findViewById(R.id.minSdk);

        final String[] versions = {
                "1 (BASE/SDK 1)", "1.1 (BASE_1_1/SDK 2)", "1.5 (Cupcake/SDK 3)", "1.6 (Donut/SDK 4)",
                "2 (Eclair/SDK 5)", "2.0.1 (Eclair_0_1/SDK 6)", "2.1 (Eclair_MR1/SDK 7)", "2.2 (Froyo/SDK 8)",
                "2.3 (Gingerbread/SDK 9)", "2.3.3 (Gingerbread_MR1/SDK 10)", "3 (Honeycomb/SDK 11)",
                "3.1 (Honeycomb_MR1/SDK 12)", "3.2 (Honeycomb_MR2/SDK 13)", "4 (Ice Cream Sandwich/SDK 14)",
                "4.0.3 (Ice Cream Sandwich_MR1/SDK 15)", "4.1 (Jellybean/SDK 16)", "4.2 (Jellybean_MR1/SDK 17)",
                "4.3 (Jellybean_MR2/SDK 18)", "4.4 (Kitkat/SDK 19)", "4.4W (Kitkat Watch/SDK 20)",
                "5 (Lollipop/SDK 21)", "5.1 (Lollipop_MR1/SDK 22)", "6 (Marshmallow/SDK 23)",
                "7 (Nougat/SDK 24)", "7.1 (Nougat_MR1/SDK 25)", "8 (Oreo/SDK 26)",
                "8.1 (Oreo_MR1/SDK 27)", "9 (Pie/SDK 28)", "10 (Q/SDK 29)",
                "11 (R/SDK 30)", "12 (S/SDK 31)", "12L (S_V2/SDK 32)", "13 (Tiramisu/SDK 33)",
                "14 (Upside Down Cake/SDK 34)", "15 (Vanilla Ice Cream/SDK 35)", "16 (Baklava/SDK 36)", "17 (Cinnamon Bun/SDK 37)"
        };

        String minSdkVersion = "", targetSdkVersion = "", verCode = "", verName = "", appName = "";
        boolean foundMinSdk = false;
        for (XMLEntry e : entries) {
            String tag = e.getTag();
            if (tag.contains("android:installLocation")) installLoc = e.getValue();
            else if (tag.contains("android:label")) appNameInput.setText(appName = e.getValue());
            else if (tag.contains("android:versionCode")) verCodeInput.setText(verCode = e.getValue());
            else if (tag.contains("android:versionName")) verNameInput.setText(verName = e.getValue());
            else if (!foundMinSdk && tag.contains("android:minSdkVersion")) {
                foundMinSdk = true;
                int minSdkVer = Integer.parseInt(minSdkVersion = e.getValue());
                minSdk.setText(rss.getString(R.string.android_ver_text, versions[minSdkVer-1], minSdkVer));
            }
            else if (tag.contains("android:targetSdkVersion")) {
                int targetSdkVer = Integer.parseInt(targetSdkVersion = e.getValue());
                targetSdk.setText(rss.getString(R.string.android_ver_text, versions[targetSdkVer-1], targetSdkVer));
            }
        }
        final String[] minSdkVersionSelected = new String[1];
        final String[] targetSdkVersionSelected = new String[1];
        minSdk.setOnItemClickListener((parent, view, position, id) -> minSdkVersionSelected[0] = (position+1) +"");
        targetSdk.setOnItemClickListener((parent, view, position, id) -> targetSdkVersionSelected[0] = (position+1) +"");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.dropdownitem, versions) {
            @NonNull
            @Override
            public View getView(int position1, @Nullable View convertView, @NonNull ViewGroup parent1) {
                if (convertView == null)
                    convertView = LayoutInflater.from(context).inflate(R.layout.dropdownitem, parent1, false);
                TextView view1 = (TextView) convertView;
                view1.setText(rss.getString(R.string.android_ver_text, versions[position1], position1 + 1));
                return convertView;
            }
        };
        targetSdk.setAdapter(adapter);
        minSdk.setAdapter(adapter);
        String[] items = rss.getStringArray(R.array.install_locations);
        if("".equals(installLoc)) installLocationTextView.setText(items[3]);
        else installLocationTextView.setText(items[Integer.parseInt(installLoc)]);

        final String[] installLocationSelected = new String[1];
        installLocationTextView.setOnItemClickListener((parent, view, position, id) -> installLocationSelected[0] = position +"");
        installLocationTextView.setAdapter(new ArrayAdapter<String>(context, R.layout.dropdownitem, items) {
            @NonNull @Override
            public View getView(int position1, @Nullable View convertView, @NonNull ViewGroup parent1) {
                if (convertView == null)
                    convertView = LayoutInflater.from(context).inflate(R.layout.dropdownitem, parent1, false);
                TextView view1 = (TextView) convertView;
                view1.setText(items[position1]);
                return convertView;
            }
        });

        quickEditDialog.findViewById(R.id.editall).setOnClickListener(v -> editAllManifestEntries(apkFile));

        String finalAppName = appName;
        String finalVerCode = verCode;
        String finalVerName = verName;
        String finalTargetSdkVersion = targetSdkVersion;
        String finalMinSdkVersion = minSdkVersion;
        String finalInstallLoc = installLoc;
        final boolean[] sign = new boolean[1];
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        MaterialCheckBox autosign = quickEditDialog.findViewById(R.id.autosign);
        autosign.setChecked(sign[0] = settings.getBoolean("autosign", true));
        autosign.setOnCheckedChangeListener((buttonView, isChecked) -> settings.edit().putBoolean("autosign", sign[0] = isChecked).apply());
        quickEditDialog.findViewById(R.id.sign_settings).setOnClickListener(uiHelper.showSignSettingsDialog());

        AlertDialog menuDialog = dialogUtil.getDialogBuilder()
                .setCustomTitle(uiHelper.getTitle(rss.getString(R.string.me_fast_attrs)))
                .setPositiveButton(rss.getString(R.string.done), (dialog, which) -> {
                    CharSequence appNameInputText = appNameInput.getText();
                    String appNameSelected = TextUtils.isEmpty(appNameInputText) ? "" : appNameInputText.toString();
                    boolean appNameChanged = (!finalAppName.equals(appNameSelected));

                    CharSequence verCodeInputText = verCodeInput.getText();
                    String verCodeSelected = TextUtils.isEmpty(verCodeInputText) ? "" : verCodeInputText.toString();
                    boolean verCodeChanged = (!finalVerCode.equals(verCodeSelected));

                    CharSequence verNameInputText = verNameInput.getText();
                    String verNameSelected = TextUtils.isEmpty(verNameInputText) ? "" : verNameInputText.toString();
                    boolean verNameChanged = (!finalVerName.equals(verNameSelected));

                    boolean minSdkVersionChanged = minSdkVersionSelected[0] != null && (!finalMinSdkVersion.equals(minSdkVersionSelected[0]));
                    boolean targetSdkVersionChanged = targetSdkVersionSelected[0] != null && (!finalTargetSdkVersion.equals(targetSdkVersionSelected[0]));

                    boolean installLocationChanged = installLocationSelected[0] != null && !installLocationSelected[0].equals(finalInstallLoc);
                    StringBuilder sb = new StringBuilder();
                    String[] options = {
                            rss.getString(R.string.me_icon),
                            rss.getString(R.string.appname),
                            rss.getString(R.string.install_location),
                            rss.getString(R.string.version_code),
                            rss.getString(R.string.version_name),
                            rss.getString(R.string.me_min_sdk_v),
                            rss.getString(R.string.me_target_sdk_v),
                            rss.getString(R.string.me_edit_all)
                    };

                    if(appNameChanged) sb.append(options[1]).append(", ");
                    if(installLocationChanged) sb.append(options[2]).append(", ");
                    if(verCodeChanged) sb.append(options[3]).append(", ");
                    if(verNameChanged) sb.append(options[4]).append(", ");
                    if(minSdkVersionChanged) sb.append(options[5]).append(", ");
                    if(targetSdkVersionChanged) sb.append(options[6]);
                    sb.append(rss.getString(R.string.me_updated));
                    SignWrapper[] wrapper = new SignWrapper[1];
                    Runnable doEdit = () -> {
                        ProgressManager pm = new ProgressManager(context, true).show();
                        pm.setText(rss.getString(R.string.saving));

                        new RunUtil(context.handler, context, sb)
                                .runInBackground(() -> {
                                    try {
                                        boolean foundMinSdk2 = false;
                                        boolean foundInstallLocation = false;
                                        int entryToRemove = 0;
                                        for (int i = 0, entriesSize = entries.size(); i < entriesSize; i++) {
                                            XMLEntry e = entries.get(i);
                                            String tag = e.getTag();
                                            if (appNameChanged && tag.contains("android:label"))
                                                e.setValue(appNameSelected);
                                            else if (verCodeChanged && tag.contains("android:versionCode"))
                                                e.setValue(verCodeSelected);
                                            else if (verNameChanged && tag.contains("android:versionName"))
                                                e.setValue(verNameSelected);
                                            else if (!foundMinSdk2 && minSdkVersionChanged && tag.contains("android:minSdkVersion")) {
                                                foundMinSdk2 = true;
                                                e.setValue(minSdkVersionSelected[0]);
                                            } else if (targetSdkVersionChanged && tag.contains("android:targetSdkVersion"))
                                                e.setValue(targetSdkVersionSelected[0]);
                                            else if (installLocationChanged && tag.contains("android:installLocation")) {
                                                foundInstallLocation = true;
                                                if (installLocationSelected[0].isEmpty()) entryToRemove = i;
                                                else e.setValue(installLocationSelected[0]);
                                            }
                                        }
                                        if(installLocationChanged) {
                                            if(foundInstallLocation) {
                                                if (entryToRemove != 0) entries.remove(entryToRemove);
                                            } else entries.add(4, new XMLEntry("android:installLocation", "=\"", installLocationSelected[0], "\""));
                                        }

                                        if (UiPrefs.genBackup(context)) {
                                            try {
                                                FileUtils.copyFile(apkFile, new File(apkFile.getPath() + ".bak"));
                                            } catch (Exception ignored) {
                                            }
                                        }
                                        writeManifestEntries(apkFile, entries);
                                        if(sign[0]) wrapper[0].signApk(apkFile);
                                        pm.dismiss();
                                        return true;
                                    } catch (Exception e) {
                                        pm.dismiss();
                                        new ErrorUtil(context).showError(e);
                                        return false;
                                    }
                                });
                    };
                    if(sign[0]) SignWrapper.requireAuth(context, sw -> {
                        wrapper[0] = sw;
                        doEdit.run();
                    }); else doEdit.run();

                })
                .setNegativeButton(android.R.string.cancel, null)
                .setView(quickEditDialog)
                .create();
        dialogUtil.styleAlertDialog(menuDialog);
    }

    public void editLauncherIcon(File apkFile) {
        FilePickerDialog.Properties properties = new FilePickerDialog.Properties();
        properties.selection_mode = FilePickerDialog.SINGLE_MODE;
        properties.selection_type = FilePickerDialog.FILE_SELECT;
        properties.root = new File(Environment.getExternalStorageDirectory().getPath());
        properties.offset = new File(Environment.getExternalStorageDirectory().getPath());
        properties.preferenceKey = "icon";
        properties.extensions = new String[]{"png", "webp", "jpg", "jpeg"};
        FilePickerDialog fpd = new FilePickerDialog(context, properties);
        fpd.setTitle(context.rss.getString(R.string.select_icon));
        ProgressManager pm = new ProgressManager(context, true);

        fpd.setDialogSelectionListener(files -> new Thread(() -> {
            String iconPath = findIconPathInManifest(apkFile);

            try (InputStream is = FileUtils.getInputStream(files[0])) {
                pm.show().setText(context.rss.getString(R.string.adding, files[0]));
                replaceZipEntry(apkFile,
                        iconPath != null ? iconPath : "res/mipmap-xxhdpi-v4/ic_launcher.png",
                        is);
                pm.dismiss();
                Extensions.showMessage(context, R.string.icon_changed);
            } catch (Exception e) {
                pm.dismiss();
                context.handler.post(() -> new ErrorUtil(context).showError(e));
            }
        }).start());
        fpd.show();
    }

    private String findIconPathInManifest(File apkFile) {
        try (ZipFile zf = new ZipFile(apkFile)) {
            FileHeader manifestEntry = zf.getFileHeader("AndroidManifest.xml");
            if (manifestEntry == null) return null;
            try (InputStream mis = zf.getInputStream(manifestEntry)) {
                APKParser apkParser = new APKParser();
                apkParser.parse(apkFile.getPath(), context);
                List<ResEntry> decodedResources = apkParser.getDecodedResources();

                List<XMLEntry> entries = new aXMLDecoder(mis, decodedResources).decode();
                for (XMLEntry e : entries) {
                    if (e.getTag().contains("android:icon")) {
                        String val = e.getValue();
                        if (val.startsWith("res/")) return val;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public void editAllManifestEntries(File apkFile) {
        new Thread(() -> {
            try {
                List<XMLEntry> entries = decodeManifest(apkFile);
                if (entries == null) {
                    Extensions.showMessage(context, R.string.could_not_decode_am);
                    return;
                }
                context.handler.post(() -> showManifestTreeDialog(apkFile, entries));
            } catch (Exception e) {
                new ErrorUtil(context).showError(e);
            }
        }).start();
    }

    @SuppressLint("SetTextI18n")
    private void showManifestTreeDialog(File apkFile, List<XMLEntry> entries) {
        ListView listView = new ListView(context);
        listView.setDividerHeight(1);

        BaseAdapter adapter = new BaseAdapter() {
            @Override public int getCount()          { return entries.size(); }
            @Override public Object getItem(int p)   { return entries.get(p); }
            @Override public long getItemId(int p)   { return p; }

            @Override
            public View getView(int pos, View convertView, ViewGroup parent) {
                LinearLayout row;
                TextView label;
                CheckBox disableChk;
                ImageView editBtn;

                if (convertView == null) {
                    row = new LinearLayout(context);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(8, 8, 8, 8);

                    disableChk = new CheckBox(context);
                    disableChk.setTag("chk");
                    disableChk.setPadding(0, 0, 8, 0);

                    label = new TextView(context);
                    label.setTag("lbl");
                    label.setTextSize(12);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    label.setLayoutParams(lp);
                    label.setSingleLine(false);
                    ColorUtil.setTextViewColor(label, Color.WHITE);

                    editBtn = new ImageView(context);
                    editBtn.setTag("btn");
                    editBtn.setImageResource(android.R.drawable.ic_menu_edit);
                    editBtn.setPadding(8, 0, 0, 0);

                    row.addView(disableChk);
                    row.addView(label);
                    row.addView(editBtn);
                    convertView = row;
                } else {
                    row        = (LinearLayout) convertView;
                    disableChk = row.findViewWithTag("chk");
                    label      = row.findViewWithTag("lbl");
                    editBtn    = row.findViewWithTag("btn");
                }

                XMLEntry entry = entries.get(pos);
                String text = entry.getText();
                label.setText(text);

                boolean isDisabled = isDisabledEntry(entry);
                disableChk.setOnCheckedChangeListener(null);
                disableChk.setChecked(isDisabled);
                disableChk.setOnCheckedChangeListener((btn, checked) -> {
                    toggleDisabled(entry, checked);
                    label.setText(entry.getText());
                });

                editBtn.setOnClickListener(v -> showEntryEditDialog(apkFile, entries, entry));

                return convertView;
            }
        };

        listView.setAdapter(adapter);

        AlertDialog d = dialogUtil.getDialogBuilder()
                .setCustomTitle(uiHelper.getTitle(rss.getString(R.string.me_edit_entries)))
                .setView(listView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(rss.getString(R.string.me_save_all), (dlg, w) ->
                        new RunUtil(context.handler, context, rss.getString(R.string.me_manifest_saved))
                                .runInBackground(() -> {
                                    try {
                                        writeManifestEntries(apkFile, entries);
                                        return true;
                                    } catch (Exception e) {
                                        new ErrorUtil(context).showError(e);
                                        return false;
                                    }
                                }))
                .create();
        dialogUtil.styleAlertDialog(d);
    }

    private boolean isDisabledEntry(XMLEntry entry) {
        String mid = entry.getMiddleTag();
        String tag = entry.getTag();
        return (mid != null && mid.contains("_disabled"))
                || (tag != null && tag.contains("_disabled"));
    }

    private void toggleDisabled(XMLEntry entry, boolean disable) {
        String current = entry.getValue();
        if (current == null) current = "";
        if (disable) {
            if (!current.startsWith("__DISABLED__")) {
                entry.setValue("__DISABLED__" + current);
            }
        } else {
            if (current.startsWith("__DISABLED__")) {
                entry.setValue(current.substring("__DISABLED__".length()));
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void showEntryEditDialog(File apkFile,
                                     List<XMLEntry> entries,
                                     XMLEntry entry) {

        String rawValue = entry.getValue().replace("__DISABLED__", "");

        EditText input = new EditText(context);
        input.setText(rawValue);
        uiHelper.styleEditText(input);

        AlertDialog d = dialogUtil.getDialogBuilder()
                .setCustomTitle(uiHelper.getTitle(
                        rss.getString(R.string.me_edit_prefix, entry.getMiddleTag().trim())))
                .setView(UiFields.wrap(context, input, null, 16))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dlg, w) -> {
                    String newVal = input.getText().toString();
                    boolean wasDisabled = entry.getValue().startsWith("__DISABLED__");
                    entry.setValue(wasDisabled ? "__DISABLED__" + newVal : newVal);
                })
                .create();
        dialogUtil.styleAlertDialog(d);
    }

    private List<XMLEntry> decodeManifest(File apkFile) {
        try (ZipFile zf = new ZipFile(apkFile)) {
            FileHeader manifestEntry = zf.getFileHeader("AndroidManifest.xml");
            if (manifestEntry == null) return null;
            try (InputStream is = zf.getInputStream(manifestEntry)) {
                return new aXMLDecoder(is).decode();
            }
        } catch (Exception e) {
            return null;
        }
    }

    public String readManifestAttrValue(File apkFile, String attrName) {
        List<XMLEntry> entries = decodeManifest(apkFile);
        if (entries == null) return "";
        for (XMLEntry e : entries) {
            if (e.getTag().contains(attrName)) return e.getValue();
        }
        return "";
    }

    public void writeManifestAttrValue(File apkFile, String attrName, String newValue) throws Exception {
        List<XMLEntry> entries = decodeManifest(apkFile);
        if (entries == null) throw new IOException(rss.getString(R.string.me_decode_fail));

        boolean found = false;
        for (XMLEntry e : entries) {
            if (e.getTag().contains(attrName.split(":")[1])) {
                e.setValue(newValue);
                found = true;
            }
        }
        if (!found) {
            throw new IOException(rss.getString(R.string.me_attr_missing, attrName));
        }
        writeManifestEntries(apkFile, entries);
    }

    public void removeManifestAttr(File apkFile, String attrName) throws Exception {
        List<XMLEntry> entries = decodeManifest(apkFile);
        if (entries == null) throw new IOException(rss.getString(R.string.me_decode_fail));
        for (int i = 0, listSize = entries.size(); i < listSize; i++) {
            XMLEntry item = entries.get(i);
            if (item.getTag().contains(attrName)) entries.remove(i);
        }
        writeManifestEntries(apkFile, entries);
    }

    /** Single-permission removal (legacy callers). Uses the safe rebuild path. */
    public void removeManifestPermission(File apkFile, String perm) throws Exception {
        Set<String> set = new HashSet<>();
        set.add(perm);
        ProgressManager pm = new ProgressManager(context, true).show();
        APKLogger logger = pm.getLogger();
        try {
            stripPermissionsViaRebuild(apkFile, set, logger);
        } finally {
            try { logger.close(); } catch (Exception ignored) {}
            pm.dismiss();
        }
    }

    /**
     * Reliable permission editor: decompiles the APK to a temp dir, edits
     * AndroidManifest.xml as plain text, rebuilds with REAndroid, then copies
     * the result back. Avoids aXMLEncoder bugs that corrupt the binary manifest.
     */
    public void showPermissionsDialog(File apkFile) {
        ProgressManager pm = new ProgressManager(context, true).show();
        new Thread(() -> {
            String[] perms;
            try {
                PackageInfo pi = context.getPackageManager().getPackageArchiveInfo(
                        apkFile.getPath(), PackageManager.GET_PERMISSIONS);
                perms = pi == null ? null : pi.requestedPermissions;
                if (perms == null || perms.length == 0) throw new IOException("No permissions found");
            } catch (Exception e) {
                pm.dismiss();
                new ErrorUtil(context).showError(e);
                return;
            }
            String[] labels = new String[perms.length];
            for (int i = 0; i < perms.length; i++) {
                boolean dangerous = false;
                try {
                    int level = context.getPackageManager().getPermissionInfo(perms[i], 0).protectionLevel
                            & PermissionInfo.PROTECTION_MASK_BASE;
                    dangerous = level == PermissionInfo.PROTECTION_DANGEROUS;
                } catch (Exception ignored) {
                }
                labels[i] = perms[i] + (dangerous ? " (dangerous)" : "");
            }
            boolean[] keep = new boolean[perms.length];
            Arrays.fill(keep, true);
            pm.dismiss();
            context.handler.post(() -> {
                AlertDialog dialog = dialogUtil.getDialogBuilder()
                        .setTitle(rss.getString(R.string.me_perms_n, perms.length))
                        .setMessage("Uncheck permissions to remove. The APK will be decompiled, edited, and rebuilt.")
                        .setMultiChoiceItems(labels, keep, (d, which, isChecked) -> keep[which] = isChecked)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(rss.getString(R.string.me_remove_unchecked), (d, which) -> {
                            SignWrapper[] wrapper = new SignWrapper[1];
                            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                            boolean[] sign = {settings.getBoolean("autosign", true)};

                            Set<String> toRemove = new HashSet<>();
                            for (int i = 0; i < perms.length; i++) {
                                if (!keep[i]) toRemove.add(perms[i]);
                            }
                            if (toRemove.isEmpty()) {
                                Extensions.showMessage(context, "Nothing to remove");
                                return;
                            }

                            ProgressManager pm2 = new ProgressManager(context, true).show();
                            pm2.setText("Rebuilding APK...");
                            APKLogger logger = pm2.getLogger();

                            Runnable doEdit = () -> {
                                new Thread(() -> {
                                    try {
                                        stripPermissionsViaRebuild(apkFile, toRemove, logger);
                                        if (sign[0] && wrapper[0] != null) wrapper[0].signApk(apkFile);
                                        try { logger.close(); } catch (Exception ignored) {}
                                        context.handler.post(pm2::dismiss);
                                        context.handler.post(() -> {
                                            Extensions.showMessage(context,
                                                    "Rebuilt APK without " + toRemove.size() + " permission(s)");
                                            context.loadFolderInPane(apkFile.getParentFile(), true);
                                        });
                                    } catch (Exception e) {
                                        try { logger.close(); } catch (Exception ignored) {}
                                        context.handler.post(pm2::dismiss);
                                        context.handler.post(() -> new ErrorUtil(context).showError(e));
                                    }
                                }).start();
                            };
                            if (sign[0]) SignWrapper.requireAuth(context, sw -> {
                                wrapper[0] = sw;
                                doEdit.run();
                            });
                            else doEdit.run();
                        }).create();
                dialogUtil.styleAlertDialog(dialog);
            });
        }).start();
    }

    /**
     * Decompile → edit manifest text → rebuild → replace original.
     * Caller supplies the APKLogger (from a shown ProgressManager).
     */
    private void stripPermissionsViaRebuild(File apkFile, Set<String> permsToRemove,
                                            APKLogger logger) throws Exception {
        File tempDir = new File(context.getCacheDir(), "perm_edit_" + System.currentTimeMillis());
        try {
            if (!tempDir.mkdirs() && !tempDir.isDirectory()) {
                throw new IOException("Cannot create temp dir");
            }

            // 1) Decompile to temp dir
            DecompileOptions dopt = new DecompileOptions();
            dopt.inputFile = apkFile;
            dopt.outputFile = tempDir;
            dopt.frameworkVersion = 35;
            dopt.type = "xml";
            dopt.loadDex = 0;
            dopt.dex = false;
            dopt.noDexDebug = true;
            dopt.force = true;
            dopt.validateResDir = true;
            dopt.splitJson = true;
            Decompiler decompiler = dopt.newCommandExecutor(logger);
            decompiler.setEnableLog(true);
            decompiler.runCommand();

            File manifestFile = new File(tempDir, "AndroidManifest.xml");
            if (!manifestFile.exists()) {
                throw new IOException("AndroidManifest.xml not found after decompile");
            }

            // 2) Edit manifest text
            String content = readTextFile(manifestFile);
            Set<String> shortNames = new HashSet<>();
            for (String p : permsToRemove) {
                String s = shortPermName(p);
                if (!s.isEmpty()) shortNames.add(s);
            }
            String edited = removePermissionsFromManifestText(content, shortNames);
            if (edited == null) {
                throw new IOException("No matching <uses-permission> found in manifest");
            }
            writeTextFile(manifestFile, edited);

            // 3) Rebuild
            File rebuilt = new File(tempDir, "rebuilt.apk");
            BuildOptions bopt = new BuildOptions();
            bopt.inputFile = tempDir;
            bopt.outputFile = rebuilt;
            bopt.type = BuildOptions.TYPE_XML;
            bopt.force = true;
            bopt.validateResDir = true;
            bopt.noCache = true;
            new Builder(bopt, logger).runCommand();

            if (!rebuilt.exists() || rebuilt.length() == 0) {
                throw new IOException("Rebuild failed: output APK not produced");
            }

            // 4) Backup original + replace
            if (UiPrefs.genBackup(context)) {
                try { FileUtils.copyFile(apkFile, new File(apkFile.getPath() + ".bak")); }
                catch (Exception ignored) {}
            }
            FileUtils.copyFile(rebuilt, apkFile);

            // 5) Caller signs after this method returns
        } finally {
            deleteRecursively(tempDir);
        }
    }

    /**
     * Removes all <uses-permission> tags whose android:name short-form matches
     * any entry in shortNames. Returns null when nothing was removed.
     */
    private static String removePermissionsFromManifestText(String content, Set<String> shortNames) {
        StringBuilder result = new StringBuilder(content.length());
        int i = 0;
        int removed = 0;
        while (i < content.length()) {
            int start = content.indexOf("<uses-permission", i);
            if (start < 0) {
                result.append(content, i, content.length());
                break;
            }
            result.append(content, i, start);
            int end = content.indexOf('>', start);
            if (end < 0) {
                result.append(content, start, content.length());
                break;
            }
            String tag = content.substring(start, end + 1);
            boolean shouldRemove = false;
            int nameIdx = tag.indexOf("android:name=\"");
            if (nameIdx >= 0) {
                int valStart = nameIdx + "android:name=\"".length();
                int valEnd = tag.indexOf('"', valStart);
                if (valEnd > valStart) {
                    String permName = tag.substring(valStart, valEnd);
                    String shortName = shortPermName(permName);
                    if (shortNames.contains(shortName)) {
                        shouldRemove = true;
                    }
                }
            }
            if (shouldRemove) {
                removed++;
            } else {
                result.append(tag);
            }
            i = end + 1;
        }
        return removed > 0 ? result.toString() : null;
    }

    public void showManifestTogglesDialog(File apkFile) {
        String[] attrs = {"android:debuggable", "android:allowBackup", "android:usesCleartextTraffic",
                "android:requestLegacyExternalStorage", "android:largeHeap"};
        String[] labels = {rss.getString(R.string.me_tog_debug), rss.getString(R.string.me_tog_backup), rss.getString(R.string.me_tog_cleartext), rss.getString(R.string.me_tog_legacy), rss.getString(R.string.me_tog_heap)};
        ProgressManager pm = new ProgressManager(context, true).show();
        new Thread(() -> {
            boolean[] current = new boolean[attrs.length];
            try {
                List<XMLEntry> entries = decodeManifest(apkFile);
                if (entries == null) throw new IOException(rss.getString(R.string.me_decode_fail));
                for (int i = 0; i < attrs.length; i++) {
                    String key = attrs[i].split(":")[1];
                    for (XMLEntry e : entries) {
                        if (e.getTag().contains(key)) {
                            current[i] = "true".equalsIgnoreCase(e.getValue());
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                pm.dismiss();
                new ErrorUtil(context).showError(e);
                return;
            }
            pm.dismiss();
            context.handler.post(() -> {
                LinearLayout root = new LinearLayout(context);
                root.setOrientation(LinearLayout.VERTICAL);
                int pad = (int) (16 * context.getResources().getDisplayMetrics().density + 0.5f);
                root.setPadding(pad, pad / 2, pad, pad / 2);
                CheckBox[] boxes = new CheckBox[attrs.length];
                for (int i = 0; i < attrs.length; i++) {
                    boxes[i] = new CheckBox(context);
                    boxes[i].setText(labels[i]);
                    boxes[i].setChecked(current[i]);
                    root.addView(boxes[i]);
                }
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                boolean[] sign = new boolean[1];
                CheckBox autosign = new CheckBox(context);
                autosign.setText(rss.getString(R.string.auto_sign));
                autosign.setChecked(sign[0] = settings.getBoolean("autosign", true));
                autosign.setOnCheckedChangeListener((b, c) -> settings.edit().putBoolean("autosign", sign[0] = c).apply());
                root.addView(autosign);
                dialogUtil.styleAlertDialog(dialogUtil.getDialogBuilder()
                        .setTitle(rss.getString(R.string.me_toggles))
                        .setView(root)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(rss.getString(R.string.apply), (dialog, which) -> {
                            SignWrapper[] wrapper = new SignWrapper[1];
                            Runnable doEdit = () -> {
                                ProgressManager pm2 = new ProgressManager(context, true).show();
                                new Thread(() -> {
                                    try {
                                        int changed = 0;
                                        for (int i = 0; i < attrs.length; i++) {
                                            if (boxes[i].isChecked() == current[i]) continue;
                                            try {
                                                writeManifestAttrValue(apkFile, attrs[i], Boolean.toString(boxes[i].isChecked()));
                                                changed++;
                                            } catch (Exception ignored) {
                                            }
                                        }
                                        if (sign[0]) wrapper[0].signApk(apkFile);
                                        pm2.dismiss();
                                        int done = changed;
                                        context.handler.post(() -> {
                                            Extensions.showMessage(context, rss.getString(R.string.me_toggles_applied, done));
                                            context.loadFolderInPane(apkFile.getParentFile(), true);
                                        });
                                    } catch (Exception e) {
                                        pm2.dismiss();
                                        new ErrorUtil(context).showError(e);
                                    }
                                }).start();
                            };
                            if (sign[0]) SignWrapper.requireAuth(context, sw -> {
                                wrapper[0] = sw;
                                doEdit.run();
                            });
                            else doEdit.run();
                        }).create());
            });
        }).start();
    }

    private void writeManifestEntries(File apkFile, List<XMLEntry> entries) throws Exception {
        List<XMLEntry> toWrite = new ArrayList<>();
        for (XMLEntry e : entries) {
            String v = e.getValue();
            if (v != null && v.startsWith("__DISABLED__")) continue;
            toWrite.add(e);
        }
        replaceZipEntry(apkFile, "AndroidManifest.xml", new aXMLEncoder().encodeString(toWrite, context));
    }

    private String appendDisabled(String middleTag) {
        int eqIdx = middleTag.lastIndexOf('=');
        if (eqIdx > 0) {
            return middleTag.substring(0, eqIdx) + "_disabled" + middleTag.substring(eqIdx);
        }
        return middleTag.trim().isEmpty() ? middleTag : middleTag + "_disabled";
    }

    private void replaceZipEntry(File apkFile, String entryPath, byte[] newBytes)
            throws IOException {
        ZipParameters zp = new ZipParameters();
        zp.setCompressionMethod(CompressionMethod.STORE);
        zp.setFileNameInZip(entryPath);
        try (ZipFile sourceZip = new ZipFile(apkFile); InputStream is = new ByteArrayInputStream(newBytes)) {
            sourceZip.addStream(is, zp);
        }
    }

    private void replaceZipEntry(File apkFile, String entryPath, InputStream is)
            throws IOException {
        ZipParameters zp = new ZipParameters();
        zp.setCompressionMethod(CompressionMethod.STORE);
        zp.setFileNameInZip(entryPath);
        try (ZipFile sourceZip = new ZipFile(apkFile)) {
            sourceZip.addStream(is, zp);
        }
    }

    private static String readTextFile(File f) throws IOException {
        try (InputStream is = FileUtils.getInputStream(f);
             BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        }
    }

    private static void writeTextFile(File f, String text) throws IOException {
        try (OutputStream os = FileUtils.getOutputStream(f)) {
            os.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void deleteRecursively(File f) {
        if (f == null) return;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteRecursively(k);
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
