package com.anhtran.securemdm;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Keeps the module boundaries described in docs/adr/0004-modular-backend.md. */
@AnalyzeClasses(
    packages = "com.anhtran.securemdm",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTests {
  private static final String MODULES = "com.anhtran.securemdm.src..";

  @ArchTest
  static final ArchRule modulesHaveNoCycles =
      slices().matching("com.anhtran.securemdm.src.(*)..").should().beFreeOfCycles();

  @ArchTest
  static final ArchRule topLevelPackagesHaveNoCycles =
      slices().matching("com.anhtran.securemdm.(*)..").should().beFreeOfCycles();

  @ArchTest
  static final ArchRule sharedCodeDoesNotDependOnModules =
      noClasses()
          .that()
          .resideInAnyPackage(
              "..securemdm.config..",
              "..securemdm.security..",
              "..securemdm.exception..",
              "..securemdm.utils..",
              "..securemdm.common..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(MODULES)
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule controllersDoNotUseRepositories =
      noClasses()
          .that()
          .resideInAPackage("..controller..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..repository..")
          .allowEmptyShould(true);
}
