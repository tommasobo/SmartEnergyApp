package ch.ethz.smartenergy;

import android.content.Context;
import android.graphics.Color;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

final class Utility {

    private Utility() {}

    /**
     * Reads a JSON file and return its content
     *
     * @param context context of the android App
     *
     */
    static String read(Context context) {
        try {
            FileInputStream fis = context.openFileInput("data.json");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException fileNotFound) {
            return null;
        }
    }

    /**
     * Creates a JSON file and return its content
     *
     * @param context context of the android App
     *
     */
    static boolean create(Context context, String jsonString){
        try {
            FileOutputStream fos = context.openFileOutput("data.json",Context.MODE_PRIVATE);
            if (jsonString != null) {
                fos.write(jsonString.getBytes());
            }
            fos.close();
            return true;
        } catch (IOException fileNotFound) {
            return false;
        }

    }

    /**
     * Check if a JSON file is present
     *
     * @param context context of the android App
     *
     */
    static boolean isFilePresent(Context context) {
        String path = context.getFilesDir().getAbsolutePath() + "/" + "data.json";
        File file = new File(path);
        return file.exists();
    }

    /**
     * Converts from Hex to RGB
     *
     * @param hex color in Hex
     *
     */
    static int rgb(String hex) {
        int color = (int) Long.parseLong(hex.replace("#", ""), 16);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color >> 0) & 0xFF;
        return Color.rgb(r, g, b);
    }

}
