package com.mbrlabs.mundus.importer;

import com.mbrlabs.mundus.exceptions.OsNotSupported;
import com.mbrlabs.mundus.data.settings.SettingsManager;
import com.mbrlabs.mundus.utils.Callback;
import com.mbrlabs.mundus.utils.Os;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around the command line program fbx-conv.
 *
 * Converts FBX & Collada (dae) 3D files into the g3db/g3dj format.
 * Supports Linux, Windows & Mac. Binaries must be in asset folder under fbx-conv.
 *
 * @author Marcus Brummer
 * @version 24-11-2015
 */
public class FbxConv {

    public static final int OUTPUT_FORMAT_G3DB = 0;
    public static final int OUTPUT_FORMAT_G3DJ = 1;

    private Os os;
    private ProcessBuilder pb;

    private boolean verbose = false;
    private boolean flipTexture = false;
    private int outputFormat = 0;
    private String input = null;
    private String output = null;

    public FbxConv() {
        if(SystemUtils.IS_OS_MAC) {
            os = Os.MAC;
        } else if(SystemUtils.IS_OS_WINDOWS) {
            os = Os.WINDOWS;
        } else if(SystemUtils.IS_OS_LINUX) {
            os = Os.LINUX;
        } else {
            throw new OsNotSupported();
        }

        pb = new ProcessBuilder(SettingsManager.getInstance().getSettings().getFbxConvBinary());
    }

    public void clear() {
        outputFormat = OUTPUT_FORMAT_G3DB;
        verbose = false;
        flipTexture = false;
        input = null;
        output = null;
    }

    public FbxConv input(String pathToFile) {
        this.input = pathToFile;
        return this;
    }

    public FbxConv output(String pathToFolder) {
        this.output = pathToFolder;
        return this;
    }

    public FbxConv flipTexture(boolean flip) {
        this.flipTexture = flip;
        return this;
    }

    public FbxConv verbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public FbxConv outputFormat(int format) {
        this.outputFormat = format;
        return this;
    }

    public FbxConvResult execute() {
        FbxConvResult result = new FbxConvResult();
        if(input == null || output == null) {
            result.setSuccess(false);
            result.setResultCode(FbxConvResult.RESULT_CODE_PARAM_ERROR);
        }

        if(!input.endsWith("fbx") || !input.endsWith("dae")) {
            result.setSuccess(false);
            result.setResultCode(FbxConvResult.RESULT_CODE_WRONG_INPUT_FORMAT);
        }

        // build arguments
        String outputFilename = FilenameUtils.getBaseName(input);
        List<String> args = new ArrayList<>(6);
        if(flipTexture) args.add("-f");
        if(verbose) args.add("-v");
        if(outputFormat == OUTPUT_FORMAT_G3DJ) {
            args.add("-o");
            args.add("g3dj");
            outputFilename += ".g3dj";
        } else {
            outputFilename += ".g3db";
        }

        args.add(input);
        args.add(FilenameUtils.concat(output, outputFilename));
        pb.command().addAll(args);

        // execute fbx-conv process
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            String log = IOUtils.toString(process.getInputStream());

            if(exitCode == 0 && !log.contains("ERROR")) {
                result.setSuccess(true);
            }
            result.setLog(log);
        } catch (IOException e) {
            e.printStackTrace();
            result.setResultCode(FbxConvResult.RESULT_CODE_IO_ERROR);
        } catch (InterruptedException e) {
            e.printStackTrace();
            result.setResultCode(FbxConvResult.RESULT_CODE_INTERRUPTED);
        }

        return result;
    }

    public void execute(Callback<FbxConvResult> callback) {
        new Thread() {
            @Override
            public void run() {
                FbxConvResult result = execute();
                callback.done(result);
            }
        }.start();
    }


    /**
     * Process result
     */
    public class FbxConvResult {
        public static final int RESULT_CODE_PARAM_ERROR = 0;
        public static final int RESULT_CODE_WRONG_INPUT_FORMAT = 1;
        public static final int RESULT_CODE_IO_ERROR = 2;
        public static final int RESULT_CODE_INTERRUPTED = 3;

        private String log;
        private String outputFile;
        private boolean success = false;
        private int resultCode;

        public String getLog() {
            return log;
        }

        public void setLog(String log) {
            this.log = log;
        }

        public String getOutputFile() {
            return outputFile;
        }

        public void setOutputFile(String outputFile) {
            this.outputFile = outputFile;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getResultCode() {
            return resultCode;
        }

        public void setResultCode(int resultCode) {
            this.resultCode = resultCode;
        }
    }

}