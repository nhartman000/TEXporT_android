# Test fixtures

`app/src/test/resources/` holds a sample collection and the Markdown the **browser extension** renders from it. `ExporterTest` checks that the Android exporter produces identical output.

The fixtures were generated with the extension's `src/export.js` (`serializeCollection`, `buildDoc`, `renderMarkdown`). The "Compiled YYYY-MM-DD" date is replaced by `DATE`. If the format changes on purpose, regenerate them from a checkout of the extension using the same sample collection (see the commit that added them), and update both repos together.
