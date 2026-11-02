// Runs from be/. Formats only the staged Java files, then lints the module.
export default {
  '*.java': (files) => [
    `./mvnw -q spotless:apply -DspotlessFiles=${files.join(',')}`,
    './mvnw -q checkstyle:check',
  ],
};
