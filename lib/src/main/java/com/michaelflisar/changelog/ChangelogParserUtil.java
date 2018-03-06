package com.michaelflisar.changelog;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import com.michaelflisar.changelog.Changelog;
import com.michaelflisar.changelog.internal.Constants;
import com.michaelflisar.changelog.R;
import com.michaelflisar.changelog.classes.Release;
import com.michaelflisar.changelog.classes.Row;
import com.michaelflisar.changelog.internal.ChangelogException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by flisar on 05.03.2018.
 */

class ChangelogParserUtil {

    static Changelog readChangeLogFile(Context context, Integer changelogRawFileId) throws Exception {
        Changelog changelog = null;
        if (changelogRawFileId == null) {
            changelogRawFileId = R.raw.changelog;
        }

        try {
            InputStream is = context.getResources().openRawResource(changelogRawFileId);
            if (is != null) {
                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(is, null);
                parser.nextTag();

                // 1) Create Changelog object
                changelog = new Changelog();

                // 2) Parse file into Changelog object
                parseLogNode(parser, changelog);

                // Close inputstream
                is.close();
            } else {
                Log.d(Constants.DEBUG_TAG, "Changelog.xml not found");
                throw new ChangelogException("Changelog.xml not found");
            }
        } catch (XmlPullParserException xpe) {
            Log.d(Constants.DEBUG_TAG, "XmlPullParseException while parsing changelog file", xpe);
            throw xpe;
        } catch (IOException ioe) {
            Log.d(Constants.DEBUG_TAG, "Error i/o with changelog.xml", ioe);
            throw ioe;
        }

        return changelog;
    }

    private static void parseLogNode(XmlPullParser parser, Changelog changelog) throws Exception {
        // safety checks
        if (parser == null || changelog == null) {
            return;
        }

        // 1) parse root tag
        parser.require(XmlPullParser.START_TAG, null, Constants.XML_ROOT_TAG);

        // 2) Parse all nested (=release) nodes
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String tag = parser.getName();
            if (tag.equals(Constants.XML_RELEASE_TAG)) {
                readReleaseNode(parser, changelog);
            }
        }
    }

    private static void readReleaseNode(XmlPullParser parser, Changelog changelog) throws Exception {
        // safety checks
        if (parser == null || changelog == null) {
            return;
        }

        // 1) parse release tag
        parser.require(XmlPullParser.START_TAG, null, Constants.XML_RELEASE_TAG);

        // 2) real all attributes of release tag
        String versionName = parser.getAttributeValue(null, Constants.XML_ATTR_VERSION_NAME);
        String versionCodeAsString = parser.getAttributeValue(null, Constants.XML_ATTR_VERSION_CODE);
        int versionCode = -1;
        if (versionCodeAsString != null) {
            try {
                versionCode = Integer.parseInt(versionCodeAsString);
            } catch (NumberFormatException ne) {
                Log.w(Constants.DEBUG_TAG, String.format("Could not parse versionCode value '%s' to an Integer, found following value: '%s'. Filtering based on versionCode can't work in this case!"));
            }
        }
        String date = parser.getAttributeValue(null, Constants.XML_ATTR_DATE);
        String filter = parser.getAttributeValue(null, Constants.XML_ATTR_FILTER);

        // 3) Create release element and add it to changelog object
        Release release = new Release(
                versionName,
                versionCode,
                date,
                filter
        );
        changelog.add(release);

        // 4) Parse all nested tags in release
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            // make sure we understand the tag we found
            String tag = parser.getName();
            if (Constants.VALID_RELEASE_SUB_TAGS.contains(tag)) {
                readReleaseRowNode(tag, parser, changelog, release);
            }
        }
    }

    private static void readReleaseRowNode(String tag, XmlPullParser parser, Changelog changelog, Release release) throws Exception {
        // safety checks
        if (parser == null || changelog == null) {
            return;
        }

        // 1) real all attributes of row tag
        String title = parser.getAttributeValue(null, Constants.XML_ATTR_TITLE);
        String filter = parser.getAttributeValue(null, Constants.XML_ATTR_FILTER);

        // 2) read content (=text) of row tag
        String text = null;
        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.getText();
            if (text == null) {
                throw new ChangelogException("ChangeLogText required in changeLogText node");
            }
            parser.nextTag();
        }

        // 3) create row element and add it to release element
        Row row = new Row(release, tag, title, text, filter);
        release.add(row);
    }
}