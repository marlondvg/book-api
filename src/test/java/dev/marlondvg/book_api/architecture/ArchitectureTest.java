package dev.marlondvg.book_api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * Enforces the dependency rules from AGENTS.md. Rules for layers that do not
 * exist yet allow an empty selection so they start checking once code arrives.
 */
@AnalyzeClasses(packages = "dev.marlondvg.book_api", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	private static final String DOMAIN = "dev.marlondvg.book_api.domain..";
	private static final String APPLICATION = "dev.marlondvg.book_api.application..";
	private static final String PERSISTENCE = "dev.marlondvg.book_api.infrastructure.persistence..";

	@ArchTest
	static final ArchRule domainShouldOnlyDependOnJavaAndItself = classes()
			.that().resideInAPackage(DOMAIN)
			.should().onlyDependOnClassesThat().resideInAnyPackage(DOMAIN, "java..")
			.because("the domain must be pure Java");

	@ArchTest
	static final ArchRule applicationShouldOnlyDependOnDomainAndServiceAnnotations = classes()
			.that().resideInAPackage(APPLICATION)
			.should().onlyDependOnClassesThat().resideInAnyPackage(
					APPLICATION,
					DOMAIN,
					"java..",
					"org.springframework.stereotype..",
					"org.springframework.transaction.annotation..")
			.because("application services may only use @Service and @Transactional from Spring")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule controllersShouldNotUsePersistence = noClasses()
			.that().areAnnotatedWith(RestController.class)
			.should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE, "org.springframework.data..")
			.because("controllers must call port/in use cases")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule jpaEntitiesShouldStayInPersistence = classes()
			.that().haveSimpleNameEndingWith("JpaEntity")
			.should().resideInAPackage(PERSISTENCE)
			.andShould().onlyBeAccessed().byAnyPackage(PERSISTENCE)
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule controllersShouldNotReturnDomainObjects = noMethods()
			.that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
			.should().haveRawReturnType(describe("a domain class", resideInAPackage(DOMAIN)))
			.because("controllers must return DTOs")
			.allowEmptyShould(true);
}
