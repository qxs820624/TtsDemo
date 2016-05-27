package net.rdrei.android.dirchooser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gu.option.Option;
import com.gu.option.UnitFunction;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DirectoryChooserFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DirectoryChooserFragment extends DialogFragment {
    public static final String KEY_CURRENT_DIRECTORY = "CURRENT_DIRECTORY";
    private static final String ARG_CONFIG = "CONFIG";
    private static final String TAG = DirectoryChooserFragment.class.getSimpleName();
    private String mNewDirectoryName;
    private String mInitialDirectory;

    private Option<OnFragmentInteractionListener> mListener = Option.none();

    private Button mBtnConfirm;
    private Button mBtnCancel;
    private ImageButton mBtnNavUp;
    private ImageButton mBtnCreateFolder;
    private TextView mTxtvSelectedFolder;
    private ListView mListDirectories;

    private ArrayAdapter<String> mListDirectoriesAdapter;
    private List<String> mFilenames;
    /**
     * The directory that is currently being shown.
     */
    private File mSelectedDir;
    private File mSelectedFile;
    private File[] mFilesInDir;
    private FileObserver mFileObserver;
    private DirectoryChooserConfig mConfig;

    public static boolean bGetFile = false;
    public static boolean bInitInstance = false;

    /*
    public class FileNameType {
        public String filename;
        public boolean isDirectory;
        public String GetName(){
            return filename;
        }
        public boolean GetisDirectory(){
            return isDirectory;
        }
    }
    */

    public DirectoryChooserFragment() {
        // Required empty public constructor
    }

    /**
     * To create the config, make use of the provided
     * {@link DirectoryChooserConfig#builder()}.
     *
     * @return A new instance of DirectoryChooserFragment.
     */
    public static DirectoryChooserFragment newInstance(@NonNull final DirectoryChooserConfig config,boolean bFile) {
        final DirectoryChooserFragment fragment = new DirectoryChooserFragment();
        bGetFile = bFile;
        final Bundle args = new Bundle();
        args.putParcelable(ARG_CONFIG, config);
        fragment.setArguments(args);
        bInitInstance = true;
        return fragment;
    }
    public void SetFileFlag(boolean bFile){
        bGetFile = bFile;
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mSelectedDir != null) {
            outState.putString(KEY_CURRENT_DIRECTORY, mSelectedDir.getAbsolutePath());
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() == null) {
            throw new IllegalArgumentException(
                    "You must create DirectoryChooserFragment via newInstance().");
        }
        mConfig = getArguments().getParcelable(ARG_CONFIG);

        if (mConfig == null) {
            throw new NullPointerException("No ARG_CONFIG provided for DirectoryChooserFragment " +
                    "creation.");
        }

        mNewDirectoryName = mConfig.newDirectoryName();
        mInitialDirectory = mConfig.initialDirectory();

        if (savedInstanceState != null) {
            mInitialDirectory = savedInstanceState.getString(KEY_CURRENT_DIRECTORY);
        }

        if (getShowsDialog()) {
            setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        } else {
            setHasOptionsMenu(true);
        }

        if (!mConfig.allowNewDirectoryNameModification() && TextUtils.isEmpty(mNewDirectoryName)) {
            throw new IllegalArgumentException("New directory name must have a strictly positive " +
                    "length (not zero) when user is not allowed to modify it.");
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {

        assert getActivity() != null;
        final View view = inflater.inflate(R.layout.directory_chooser, container, false);

        mBtnConfirm = (Button) view.findViewById(R.id.btnConfirm);
        mBtnCancel = (Button) view.findViewById(R.id.btnCancel);
        mBtnNavUp = (ImageButton) view.findViewById(R.id.btnNavUp);
        mBtnCreateFolder = (ImageButton) view.findViewById(R.id.btnCreateFolder);
        mTxtvSelectedFolder = (TextView) view.findViewById(R.id.txtvSelectedFolder);
        mListDirectories = (ListView) view.findViewById(R.id.directoryList);

        mBtnConfirm.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                if (isValidFile(mSelectedDir)) {
                    bInitInstance = false;
                    returnSelectedFolder();
                }else if (isValidFile(mSelectedFile)){
                    bInitInstance = false;
                    returnSelectedFolder();
                }
            }
        });

        mBtnCancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                mListener.foreach(new UnitFunction<OnFragmentInteractionListener>() {
                    @Override
                    public void apply(final OnFragmentInteractionListener listener) {
                        bInitInstance = false;
                        listener.onCancelChooser();
                    }
                });
            }
        });

        mListDirectories.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(final AdapterView<?> parent, final View view,
                    final int position, final long id) {
                debug("%d, Selected index: %d,file:%s", mFilesInDir.length, position,mFilesInDir[position]);
                if (mFilesInDir != null && position >= 0
                        && position < mFilesInDir.length) {
                    if (!bGetFile || mFilesInDir[position].isDirectory()){
                        changeDirectory(mFilesInDir[position]);
                    }else{
                        mSelectedFile = mFilesInDir[position];
                        refreshButtonState();
                    }
                }
            }
        });

        mBtnNavUp.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                final File parent;
                if (mSelectedDir != null
                        && (parent = mSelectedDir.getParentFile()) != null) {
                    changeDirectory(parent);
                }
            }
        });

        mBtnCreateFolder.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                openNewFolderDialog();
            }
        });

        if (!getShowsDialog()) {
            mBtnCreateFolder.setVisibility(View.GONE);
        }

        adjustResourceLightness();

        mFilenames = new ArrayList<>();

        mListDirectoriesAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, mFilenames);
        mListDirectories.setAdapter(mListDirectoriesAdapter);

        File initialDir = null;
        if (!TextUtils.isEmpty(mInitialDirectory) && isValidFile(new File(mInitialDirectory))) {
            initialDir = new File(mInitialDirectory);
        } else {
            if (false){
                initialDir = Environment.getExternalStorageDirectory();
            }else{
                List<File> lExtCardPath = getExtSDCardPath();
                for (File fpath :lExtCardPath){
                    initialDir = fpath;
                }
            }
        }
        changeDirectory(initialDir);
        return view;
    }

    /**
     * 获取外置SD卡路径
     * @return	应该就一条记录或空
     */
    public List<File> getExtSDCardPath()
    {
        List lResult = new ArrayList();
        try {
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec("mount");
            InputStream is = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                // Log.e("getExtSDCardPath",line);
                if (line.contains("extSdCard") || line.contains("sdcard2"))
                {
                    String [] arr = line.split(" ");
                    String path = arr[1];
                    File file = new File(path);
                    if (file.isDirectory())
                    {
                        lResult.add(file);
                    }
                }
            }
            isr.close();
        } catch (Exception e) {
        }
        return lResult;
    }

    private void adjustResourceLightness() {
        // change up button to light version if using dark theme
        int color = 0xFFFFFF;
        final Resources.Theme theme = getActivity().getTheme();

        if (theme != null) {
            final TypedArray backgroundAttributes = theme.obtainStyledAttributes(
                    new int[]{android.R.attr.colorBackground});

            if (backgroundAttributes != null) {
                color = backgroundAttributes.getColor(0, 0xFFFFFF);
                backgroundAttributes.recycle();
            }
        }

        // convert to greyscale and check if < 128
        if (color != 0xFFFFFF && 0.21 * Color.red(color) +
                0.72 * Color.green(color) +
                0.07 * Color.blue(color) < 128) {
            mBtnNavUp.setImageResource(R.drawable.navigation_up_light);
            mBtnCreateFolder.setImageResource(R.drawable.ic_action_create_light);
        }
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        if (activity instanceof OnFragmentInteractionListener) {
            mListener = Option.some((OnFragmentInteractionListener) activity);
        } else {
            Fragment owner = getTargetFragment();
            if (owner instanceof OnFragmentInteractionListener) {
                mListener = Option.some((OnFragmentInteractionListener) owner);
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mFileObserver != null) {
            mFileObserver.stopWatching();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFileObserver != null) {
            mFileObserver.startWatching();
        }
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.directory_chooser, menu);

        final MenuItem menuItem = menu.findItem(R.id.new_folder_item);

        if (menuItem == null) {
            return;
        }

        menuItem.setVisible(isValidFile(mSelectedDir) && mNewDirectoryName != null);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();

        if (itemId == R.id.new_folder_item) {
            openNewFolderDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Shows a confirmation dialog that asks the user if he wants to create a
     * new folder. User can modify provided name, if it was not disallowed.
     */
    private void openNewFolderDialog() {
        @SuppressLint("InflateParams")
        final View dialogView = getActivity().getLayoutInflater().inflate(
                R.layout.dialog_new_folder, null);
        final TextView msgView = (TextView) dialogView.findViewById(R.id.msgText);
        final EditText editText = (EditText) dialogView.findViewById(R.id.editText);
        editText.setText(mNewDirectoryName);
        msgView.setText(getString(R.string.create_folder_msg, mNewDirectoryName));

        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.create_folder_label)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel_label,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                dialog.dismiss();
                            }
                        })
                .setPositiveButton(R.string.confirm_label,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                dialog.dismiss();
                                mNewDirectoryName = editText.getText().toString();
                                final int msg = createFolder();
                                Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
                            }
                        })
                .show();

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(editText.getText().length() != 0);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence charSequence, final int i, final int i2, final int i3) {

            }

            @Override
            public void onTextChanged(final CharSequence charSequence, final int i, final int i2, final int i3) {
                final boolean textNotEmpty = charSequence.length() != 0;
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(textNotEmpty);
                msgView.setText(getString(R.string.create_folder_msg, charSequence.toString()));
            }

            @Override
            public void afterTextChanged(final Editable editable) {

            }
        });

        editText.setVisibility(mConfig.allowNewDirectoryNameModification()
                ? View.VISIBLE : View.GONE);
    }

    private static void debug(final String message, final Object... args) {
        Log.d(TAG, String.format(message, args));
    }

    /**
     * Change the directory that is currently being displayed.
     *
     * @param dir The file the activity should switch to. This File must be
     *            non-null and a directory, otherwise the displayed directory
     *            will not be changed
     */
    private void changeDirectory(final File dir) {
        if (dir == null) {
            debug("Could not change folder: dir was null");
        } else if (!dir.isDirectory()) {
            debug("Could not change folder: dir is no directory");
        } else {
            final File[] contents = dir.listFiles();
            if (contents != null) {
                int numDirectories = 0;
                for (final File f : contents) {
                    if (!bGetFile) {
                        if (f.isDirectory()) {
                            numDirectories++;
                        }
                    }else {
                        // Log.e("changeDirectory",String.valueOf(bGetFile));
                        numDirectories++;
                    }
                }
                mFilesInDir = new File[numDirectories];
                // Log.e("changeDirectory",String.valueOf(numDirectories));
                mFilenames.clear();
                for (int i = 0, counter = 0; i < numDirectories; counter++) {
                    if (!bGetFile) {
                        if (contents[counter].isDirectory()) {
                            mFilesInDir[i] = contents[counter];
                            mFilenames.add(contents[counter].getName());
                            i++;
                        }
                    }else {
                        mFilesInDir[i] = contents[counter];
                        mFilenames.add(contents[counter].getName());
                        i++;
                    }
                }

                Arrays.sort(mFilesInDir);
                Collections.sort(mFilenames);

                mSelectedDir = dir;
                mTxtvSelectedFolder.setText(dir.getAbsolutePath());
                mListDirectoriesAdapter.notifyDataSetChanged();
                mFileObserver = createFileObserver(dir.getAbsolutePath());
                mFileObserver.startWatching();
                debug("Changed directory to %s", dir.getAbsolutePath());
            } else {
                debug("Could not change folder: contents of dir were null");
            }
        }
        refreshButtonState();
    }

    /**
     * Changes the state of the buttons depending on the currently selected file
     * or folder.
     */
    private void refreshButtonState() {
        final Activity activity = getActivity();
        if (!bGetFile) {
            if (activity != null && mSelectedDir != null) {
                mBtnConfirm.setEnabled(isValidFile(mSelectedDir));
                getActivity().invalidateOptionsMenu();
            }
        }else{
            if (activity != null && mSelectedFile != null){
                mBtnConfirm.setEnabled(isValidFile(mSelectedFile));
                getActivity().invalidateOptionsMenu();
            }
        }
    }

    /**
     * Refresh the contents of the directory that is currently shown.
     */
    private void refreshDirectory() {
        if (mSelectedDir != null) {
            changeDirectory(mSelectedDir);
        }
    }

    /**
     * Sets up a FileObserver to watch the current directory.
     */
    private FileObserver createFileObserver(final String path) {
        return new FileObserver(path, FileObserver.CREATE | FileObserver.DELETE
                | FileObserver.MOVED_FROM | FileObserver.MOVED_TO) {

            @Override
            public void onEvent(final int event, final String path) {
                debug("FileObserver received event %d", event);
                final Activity activity = getActivity();

                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshDirectory();
                        }
                    });
                }
            }
        };
    }

    /**
     * Returns the selected folder as a result to the activity the fragment's attached to. The
     * selected folder can also be null.
     */
    private void returnSelectedFolder() {
        if (mSelectedDir != null) {
            debug("Returning %s as result", mSelectedDir.getAbsolutePath());
            mListener.foreach(new UnitFunction<OnFragmentInteractionListener>() {
                @Override
                public void apply(final OnFragmentInteractionListener f) {
                    if (bGetFile){
                        f.onSelectDirectory(mSelectedFile.toString());
                    }else{
                        // Log.e("returnSelectedFolder",mSelectedDir.toString());
                        f.onSelectDirectory(mSelectedDir.toString());
                    }
                }
            });
        } else {
            mListener.foreach(new UnitFunction<OnFragmentInteractionListener>() {
                @Override
                public void apply(final OnFragmentInteractionListener f) {
                    f.onCancelChooser();
                }
            });
        }

    }

    /**
     * Creates a new folder in the current directory with the name
     * CREATE_DIRECTORY_NAME.
     */
    private int createFolder() {
        if (mNewDirectoryName != null && mSelectedDir != null
                && mSelectedDir.canWrite()) {
            final File newDir = new File(mSelectedDir, mNewDirectoryName);
            if (newDir.exists()) {
                return R.string.create_folder_error_already_exists;
            } else {
                final boolean result = newDir.mkdir();
                if (result) {
                    return R.string.create_folder_success;
                } else {
                    return R.string.create_folder_error;
                }
            }
        } else if (mSelectedDir != null && !mSelectedDir.canWrite()) {
            return R.string.create_folder_error_no_write_access;
        } else {
            return R.string.create_folder_error;
        }
    }

    /**
     * Returns true if the selected file or directory would be valid selection.
     */
    private boolean isValidFile(final File file) {
        if (!bGetFile )
        return (file != null &&  file.isDirectory() && file.canRead() &&
                (mConfig.allowNewDirectoryNameModification() || file.canWrite()));
        else
            return (file != null &&  file.isFile() && file.canRead() && isText(file));
    }

    public boolean isText(final File file){
        String suffix = file.getName().substring(file.getName().lastIndexOf(".")+1);
        // Log.e(TAG,"suffix is :" + suffix);
        if (suffix.equalsIgnoreCase("txt") || suffix.equalsIgnoreCase("java")|| suffix.equalsIgnoreCase("log")|| suffix.equalsIgnoreCase("rc")
                || suffix.equalsIgnoreCase("ini")){
            return true;
        }
        else {
            return false;
        }
    }

    @Nullable
    public OnFragmentInteractionListener getDirectoryChooserListener() {
        return mListener.get();
    }

    public void setDirectoryChooserListener(@Nullable final OnFragmentInteractionListener listener) {
        mListener = Option.option(listener);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        /**
         * Triggered when the user successfully selected their destination directory.
         */
        void onSelectDirectory(@NonNull String path);

        /**
         * Advices the activity to remove the current fragment.
         */
        void onCancelChooser();
    }

}
