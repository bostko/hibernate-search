/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

import groovy.transform.Field

/*
 * See https://github.com/hibernate/hibernate-jenkins-pipeline-helpers
 */
@Library('hibernate-jenkins-pipeline-helpers@1.3')
import org.hibernate.jenkins.pipeline.helpers.job.JobHelper
import org.hibernate.jenkins.pipeline.helpers.alternative.AlternativeMultiMap
import org.hibernate.jenkins.pipeline.helpers.version.Version

/*
 * WARNING: DO NOT IMPORT LOCAL LIBRARIES HERE.
 *
 * By local, I mean libraries whose files are in the same Git repository.
 *
 * The Jenkinsfile is protected and will not be executed if modified in pull requests from external users,
 * but other local library files are not protected.
 * A user could potentially craft a malicious PR by modifying a local library.
 *
 * See https://blog.grdryn.me/blog/jenkins-pipeline-trust.html for a full explanation,
 * and a potential solution if we really need local libraries.
 * Alternatively we might be able to host libraries in a separate GitHub repo and configure
 * them in the GUI: see https://ci.hibernate.org/job/hibernate-search/configure, "Pipeline Libraries".
 */

/*
 * See https://github.com/hibernate/hibernate-jenkins-pipeline-helpers for the documentation
 * of the helpers library used in this Jenkinsfile,
 * and for help writing Jenkinsfiles.
 *
 * ### Jenkins configuration
 *
 * #### Jenkins plugins
 *
 * This file requires the following plugins in particular:
 *
 * - everything required by the helpers library (see the org.hibernate.(...) imports for a link to its documentation)
 * - https://plugins.jenkins.io/ec2 for AWS credentials
 * - https://plugins.jenkins.io/lockable-resources for the lock on the AWS Elasticsearch server
 * - https://plugins.jenkins.io/pipeline-github for the trigger on pull request comments
 *
 * #### Script approval
 *
 * If not already done, you will need to allow the following calls in <jenkinsUrl>/scriptApproval/:
 *
 * - everything required by the helpers library (see the org.hibernate.(...) imports for a link to its documentation)
 *
 * ### Integrations
 *
 * #### Nexus deployment
 *
 * This job includes two deployment modes:
 *
 * - A deployment of snapshot artifacts for every non-PR build on "primary" branches (master and maintenance branches).
 * - A full release when starting the job with specific parameters.
 *
 * In the first case, the name of a Maven settings file must be provided in the job configuration file
 * (see below).
 *
 * #### AWS
 *
 * This job will trigger integration tests against an Elasticsearch service hosted on AWS.
 *
 * You need to set some environment variables to select the endpoint (see below).
 *
 * Then you will also need to add AWS credentials in Jenkins
 * and reference them from the configuration file (see below).
 *
 * #### Coveralls (optional)
 *
 * You need to enable your repository in Coveralls first: see https://coveralls.io/repos/new.
 *
 * Then you will also need to add the Coveralls repository token as credentials in Jenkins
 * and reference it from the configuration file (see below).
 *
 * #### Sonarcloud (optional)
 *
 * You need to enable the SonarCloud GitHub app for your repository:
 * see https://github.com/apps/sonarcloud.
 *
 * Then you will also need to add SonarCloud credentials in Jenkins
 * and reference them from the configuration file (see below).
 *
 * #### Gitter (optional)
 *
 * You need to enable the Jenkins integration in your Gitter room first:
 * see https://gitlab.com/gitlab-org/gitter/webapp/blob/master/docs/integrations.md
 *
 * Then you will also need to configure *global* secret text credentials containing the Gitter webhook URL,
 * and list the ID of these credentials in the job configuration file
 * (see https://github.com/hibernate/hibernate-jenkins-pipeline-helpers#job-configuration-file).
 *
 * ### Job configuration
 *
 * This Jenkinsfile gets its configuration from four sources:
 * branch name, environment variables, a configuration file, and credentials.
 * All configuration is optional for the default build (and it should stay that way),
 * but some features require some configuration.
 *
 * #### Branch name
 *
 * See the org.hibernate.(...) imports for a link to the helpers library documentation,
 * which explains the basics.
 *
 * #### Environment variables
 *
 * The following environment variables are necessary for some features:
 *
 * - 'ES_AWS_REGION', containing the name of an AWS region such as 'us-east-1', to test Elasticsearch as a service on AWS.
 * - 'ES_AWS_<majorminor without dot>_ENDPOINT' (e.g. 'ES_AWS_52_ENDPOINT'),
 * containing the URL of an AWS Elasticsearch service endpoint using that exact version,
 * to test Elasticsearch as a service on AWS.
 *
 * #### Job configuration file
 *
 * See the org.hibernate.(...) imports for a link to the helpers library documentation,
 * which explains the basic structure of this file and how to set it up.
 *
 * Below is the additional structure specific to this Jenkinsfile:
 *
 *     aws:
 *       # String containing the ID of aws credentials. Mandatory in order to test against an Elasticsearch service hosted on AWS.
 *       # Expects username/password credentials where the username is the AWS access key
 *       # and the password is the AWS secret key.
 *       credentials: ...
 *     coveralls:
 *       # String containing the ID of coveralls credentials. Optional.
 *       # Expects secret text credentials containing the repository token.
 *       # Note these credentials should be registered at the job level, not system-wide.
 *       credentials: ...
 *     sonar:
 *       # String containing the ID of Sonar credentials. Optional.
 *       # Expects username/password credentials where the username is the organization ID on sonarcloud.io
 *       # and the password is a sonarcloud.io access token with sufficient rights for that organization.
 *       credentials: ...
 *     deployment:
 *       maven:
 *         # String containing the ID of a Maven settings file registered using the config-file-provider Jenkins plugin.
 *         # The settings must provide credentials to the servers with ID
 *         # 'jboss-releases-repository' and 'jboss-snapshots-repository'.
 *         settingsId: ...
 */

@Field final String MAVEN_TOOL = 'Apache Maven 3.6'

// Default node pattern, to be used for resource-intensive stages.
// Should not include the master node.
@Field final String NODE_PATTERN_BASE = 'Slave'
// Quick-use node pattern, to be used for very light, quick, and environment-independent stages,
// such as sending a notification. May include the master node in particular.
@Field final String QUICK_USE_NODE_PATTERN = 'Master||Slave'

@Field AlternativeMultiMap<BuildEnvironment> environments
@Field JobHelper helper

@Field boolean enableDefaultBuild = false
@Field boolean enableDefaultBuildIT = false
@Field boolean performRelease = false
@Field boolean deploySnapshot = false

@Field Version releaseVersion
@Field Version afterReleaseDevelopmentVersion

this.helper = new JobHelper(this)

helper.runWithNotification {

stage('Configure') {
	this.environments = AlternativeMultiMap.create([
			jdk: [
					// This should not include every JDK; in particular let's not care too much about EOL'd JDKs like version 9
					// See http://www.oracle.com/technetwork/java/javase/eol-135779.html
					new JdkBuildEnvironment(version: '8', buildJdkTool: 'OpenJDK 11 Latest',
							testJdkRelease: '8', testJdkTool: 'OpenJDK 8 Latest',
							condition: TestCondition.AFTER_MERGE),
					new JdkBuildEnvironment(version: '11', buildJdkTool: 'OpenJDK 11 Latest',
							condition: TestCondition.BEFORE_MERGE,
							isDefault: true),
					new JdkBuildEnvironment(version: '14', buildJdkTool: 'OpenJDK 14 Latest',
							condition: TestCondition.AFTER_MERGE),
					new JdkBuildEnvironment(version: '15', buildJdkTool: 'OpenJDK 15 Latest',
							condition: TestCondition.AFTER_MERGE),
					new JdkBuildEnvironment(version: '16', buildJdkTool: 'OpenJDK 16 Latest',
							condition: TestCondition.AFTER_MERGE)
			],
			compiler: [
					new CompilerBuildEnvironment(name: 'eclipse', mavenProfile: 'compiler-eclipse',
							condition: TestCondition.BEFORE_MERGE)
			],
			database: [
					new DatabaseBuildEnvironment(dbName: 'h2', mavenProfile: 'h2',
							condition: TestCondition.BEFORE_MERGE,
							isDefault: true),
					new DatabaseBuildEnvironment(dbName: 'mariadb', mavenProfile: 'ci-mariadb',
							condition: TestCondition.AFTER_MERGE),
					new DatabaseBuildEnvironment(dbName: 'postgresql', mavenProfile: 'ci-postgresql',
							condition: TestCondition.AFTER_MERGE)
			],
			esLocal: [
					new EsLocalBuildEnvironment(versionRange: '[5.6,6.0)', mavenProfile: 'elasticsearch-5.6',
							jdkTool: 'OpenJDK 8 Latest',
							condition: TestCondition.AFTER_MERGE),
					// ES 6.2, 6.3.0, 6.3.1 and 6.3.2 and below have a bug that prevents double-nested
					// sorts from working: https://github.com/elastic/elasticsearch/issues/32130
					new EsLocalBuildEnvironment(versionRange: '[6.0,6.2)', mavenProfile: 'elasticsearch-6.0',
							jdkTool: 'OpenJDK 8 Latest',
							condition: TestCondition.ON_DEMAND),
					// ES 6.3 has a bug that prevents IndexingIT from passing.
					// See https://github.com/elastic/elasticsearch/issues/32395
					new EsLocalBuildEnvironment(versionRange: '[6.3,6.4)', mavenProfile: 'elasticsearch-6.3',
							jdkTool: 'OpenJDK 8 Latest',
							condition: TestCondition.ON_DEMAND),
					new EsLocalBuildEnvironment(versionRange: '[6.4,6.7)', mavenProfile: 'elasticsearch-6.4',
							jdkTool: 'OpenJDK 8 Latest',
							condition: TestCondition.AFTER_MERGE),
					new EsLocalBuildEnvironment(versionRange: '[6.7,7.0)', mavenProfile: 'elasticsearch-6.7',
							jdkTool: 'OpenJDK 8 Latest',
							condition: TestCondition.AFTER_MERGE),
					// Not testing 7.0/7.1/7.2 to make the build quicker.
					// The only difference with 7.3+ is they have a bug in their BigInteger support.
					new EsLocalBuildEnvironment(versionRange: '[7.0,7.3)', mavenProfile: 'elasticsearch-7.0',
							jdkTool: 'OpenJDK 8 Latest',
							condition: TestCondition.ON_DEMAND),
					new EsLocalBuildEnvironment(versionRange: '[7.3,7.7)', mavenProfile: 'elasticsearch-7.3',
							jdkTool: 'OpenJDK 11 Latest',
							condition: TestCondition.AFTER_MERGE),
					// Not testing 7.7 to make the build quicker.
					// The only difference with 7.7+ is how we create templates for tests.
					new EsLocalBuildEnvironment(versionRange: '[7.7,7.8)', mavenProfile: 'elasticsearch-7.7',
							jdkTool: 'OpenJDK 11 Latest',
							condition: TestCondition.ON_DEMAND),
					// Not testing 7.9 to make the build quicker.
					// The only difference with 7.10+ is an additional test for exists on null values,
					// which is disabled on 7.10 but enabled on all older versions (not just 7.9).
					new EsLocalBuildEnvironment(versionRange: '[7.8,7.10)', mavenProfile: 'elasticsearch-7.8',
							jdkTool: 'OpenJDK 11 Latest',
							condition: TestCondition.AFTER_MERGE),
					new EsLocalBuildEnvironment(versionRange: '[7.10,7.x)', mavenProfile: 'elasticsearch-7.10',
							jdkTool: 'OpenJDK 11 Latest',
							condition: TestCondition.BEFORE_MERGE,
							isDefault: true)
			],
			// Note that each of these environments will only be tested if the appropriate
			// environment variable with the AWS ES Service URL is defined in CI.
			esAws: [
					new EsAwsBuildEnvironment(version: '5.6', mavenProfile: 'elasticsearch-5.6',
							condition: TestCondition.AFTER_MERGE),
					new EsAwsBuildEnvironment(version: '6.0', mavenProfile: 'elasticsearch-6.0',
							condition: TestCondition.AFTER_MERGE),
					// ES 6.2, 6.3.0, 6.3.1 and 6.3.2 and below have a bug that prevents double-nested
					// sorts from working: https://github.com/elastic/elasticsearch/issues/32130
					new EsAwsBuildEnvironment(version: '6.2', mavenProfile: 'elasticsearch-6.0',
							condition: TestCondition.ON_DEMAND),
					// ES 6.3 has a bug that prevents IndexingIT from passing.
					// See https://github.com/elastic/elasticsearch/issues/32395
					new EsAwsBuildEnvironment(version: '6.3', mavenProfile: 'elasticsearch-6.3',
							condition: TestCondition.ON_DEMAND),
					new EsAwsBuildEnvironment(version: '6.4', mavenProfile: 'elasticsearch-6.4',
							condition: TestCondition.AFTER_MERGE),
					new EsAwsBuildEnvironment(version: '6.5', mavenProfile: 'elasticsearch-6.4',
							condition: TestCondition.AFTER_MERGE),
					new EsAwsBuildEnvironment(version: '6.7', mavenProfile: 'elasticsearch-6.7',
							condition: TestCondition.AFTER_MERGE),
					new EsAwsBuildEnvironment(version: '6.8', mavenProfile: 'elasticsearch-6.7',
							condition: TestCondition.AFTER_MERGE),
					new EsAwsBuildEnvironment(version: '7.1', mavenProfile: 'elasticsearch-7.0',
							condition: TestCondition.AFTER_MERGE),
					new EsAwsBuildEnvironment(version: '7.4', mavenProfile: 'elasticsearch-7.3',
							condition: TestCondition.AFTER_MERGE),
					new EsAwsBuildEnvironment(version: '7.7', mavenProfile: 'elasticsearch-7.7',
							condition: TestCondition.AFTER_MERGE),
					new EsAwsBuildEnvironment(version: '7.8', mavenProfile: 'elasticsearch-7.8',
							condition: TestCondition.AFTER_MERGE),

					// Also test static credentials, but only for the latest version
					new EsAwsBuildEnvironment(version: '7.8', mavenProfile: 'elasticsearch-7.8',
							staticCredentials: true,
							condition: TestCondition.AFTER_MERGE)
			]
	])

	helper.configure {
		configurationNodePattern QUICK_USE_NODE_PATTERN
		file 'job-configuration.yaml'
		jdk {
			defaultTool environments.content.jdk.default.buildJdkTool
		}
		maven {
			defaultTool MAVEN_TOOL
			producedArtifactPattern "org/hibernate/search/*"
			producedArtifactPattern "org/hibernate/hibernate-search*"
		}
	}

	properties([
			buildDiscarder(
					logRotator(daysToKeepStr: '90')
			),
			pipelineTriggers(
					// HSEARCH-3417: do not add snapshotDependencies() here, this was known to cause problems.
					[
							issueCommentTrigger('.*test this please.*')
					]
							+ helper.generateUpstreamTriggers()
			),
			helper.generateNotificationProperty(),
			parameters([
					choice(
							name: 'ENVIRONMENT_SET',
							choices: """AUTOMATIC
DEFAULT
SUPPORTED
ALL""",
							description: """A set of environments that must be checked.
'AUTOMATIC' picks a different set of environments based on the branch name and whether a release is being performed.
'DEFAULT' means a single build with the default environment expected by the Maven configuration,
while other options will trigger multiple Maven executions in different environments."""
					),
					string(
							name: 'ENVIRONMENT_FILTER',
							defaultValue: '',
							trim: true,
							description: """A regex filter to apply to the environments that must be checked.
If this parameter is non-empty, ENVIRONMENT_SET will be ignored and environments whose tag matches the given regex will be checked.
Some useful filters: 'default', 'jdk', 'jdk-10', 'eclipse', 'postgresql', 'elasticsearch-local-[5.6'.
"""
					),
					string(
							name: 'RELEASE_VERSION',
							defaultValue: '',
							description: 'The version to be released, e.g. 5.10.0.Final. Setting this triggers a release.',
							trim: true
					),
					string(
							name: 'RELEASE_DEVELOPMENT_VERSION',
							defaultValue: '',
							description: 'The next version to be used after the release, e.g. 5.10.0-SNAPSHOT.',
							trim: true
					),
					booleanParam(
							name: 'RELEASE_DRY_RUN',
							defaultValue: false,
							description: 'If true, just simulate the release, without pushing any commits or tags, and without uploading any artifacts or documentation.'
					)
			])
	])

	performRelease = (params.RELEASE_VERSION ? true : false)

	if (!performRelease && helper.scmSource.branch.primary && !helper.scmSource.pullRequest) {
		if (helper.configuration.file?.deployment?.maven?.settingsId) {
			deploySnapshot = true
		}
		else {
			echo "Missing deployment configuration in job configuration file - snapshot deployment will be skipped."
		}
	}

	if (params.ENVIRONMENT_FILTER) {
		keepOnlyEnvironmentsMatchingFilter(params.ENVIRONMENT_FILTER)
	}
	else {
		keepOnlyEnvironmentsFromSet(params.ENVIRONMENT_SET)
	}

	environments.content.esAws.enabled.removeAll { buildEnv ->
		buildEnv.endpointUrl = env.getProperty(buildEnv.endpointVariableName)
		if (!buildEnv.endpointUrl) {
			echo "Skipping test ${buildEnv.tag} because environment variable '${buildEnv.endpointVariableName}' is not defined."
			return true
		}
		buildEnv.awsRegion = env.ES_AWS_REGION
		if (!buildEnv.awsRegion) {
			echo "Skipping test ${buildEnv.tag} because environment variable 'ES_AWS_REGION' is not defined."
			return true
		}
		return false // Environment is fully defined, do not remove
	}

	// Determine whether ITs need to be run in the default build
	enableDefaultBuildIT = environments.content.any { key, envSet ->
		return envSet.enabled.contains(envSet.default)
	}
	// No need to re-test default environments separately, they will be tested as part of the default build if needed
	environments.content.each { key, envSet ->
		envSet.enabled.remove(envSet.default)
	}

	enableDefaultBuild =
			enableDefaultBuildIT ||
			environments.content.any { key, envSet -> envSet.enabled.any { buildEnv -> buildEnv.requiresDefaultBuildArtifacts() } } ||
			deploySnapshot

	echo """Branch: ${helper.scmSource.branch.name}
PR: ${helper.scmSource.pullRequest?.id}
params.ENVIRONMENT_SET: ${params.ENVIRONMENT_SET}
params.ENVIRONMENT_FILTER: ${params.ENVIRONMENT_FILTER}

Resulting execution plan:
    enableDefaultBuild=$enableDefaultBuild
    enableDefaultBuildIT=$enableDefaultBuildIT
    environments=${environments.enabledAsString}
    performRelease=$performRelease
    deploySnapshot=$deploySnapshot
"""

	if (performRelease) {
		releaseVersion = Version.parseReleaseVersion(params.RELEASE_VERSION)
		echo "Inferred version family for the release to '$releaseVersion.family'"

		// Check that all the necessary parameters are set
		if (!params.RELEASE_DEVELOPMENT_VERSION) {
			throw new IllegalArgumentException(
					"Missing value for parameter RELEASE_DEVELOPMENT_VERSION." +
							" This parameter must be set when RELEASE_VERSION is set."
			)
		}
		if (!params.RELEASE_DRY_RUN && !helper.configuration.file?.deployment?.maven?.settingsId) {
			throw new IllegalArgumentException(
					"Missing deployment configuration in job configuration file." +
							" Cannot deploy artifacts during the release."
			)
		}
	}

	if (params.RELEASE_DEVELOPMENT_VERSION) {
		afterReleaseDevelopmentVersion = Version.parseDevelopmentVersion(params.RELEASE_DEVELOPMENT_VERSION)
	}
}

stage('Default build') {
	if (!enableDefaultBuild) {
		echo 'Skipping default build and integration tests in the default environment'
		helper.markStageSkipped()
		return
	}
	runBuildOnNode {
		helper.withMavenWorkspace(mavenSettingsConfig: deploySnapshot ? helper.configuration.file.deployment.maven.settingsId : null) {
			sh """ \
					mvn clean \
					--fail-at-end \
					${deploySnapshot ? "\
							deploy \
					" : "\
							install \
					"} \
					-Pdist -Pcoverage -Pjqassistant \
					${enableDefaultBuildIT ? '' : '-DskipITs'} \
					${toTestJdkArg(environments.content.jdk.default)} \
					${toElasticsearchJdkArg(environments.content.jdk.default)} \
			"""

			// Don't try to report to Coveralls.io or SonarCloud if coverage data is missing
			if (enableDefaultBuildIT) {
				if (helper.configuration.file?.coveralls?.credentials) {
					def coverallsCredentialsId = helper.configuration.file.coveralls.credentials
					withCredentials([string(credentialsId: coverallsCredentialsId, variable: 'COVERALLS_TOKEN')]) {
						sh """ \
								mvn coveralls:report \
								-DrepoToken=${COVERALLS_TOKEN} \
								${helper.scmSource.pullRequest ? """ \
										-DpullRequest=${helper.scmSource.pullRequest.id} \
								""" : """ \
										-Dbranch=${helper.scmSource.branch.name} \
								"""} \
						"""
					}
				}
				else {
					echo "No Coveralls token configured - skipping Coveralls report."
				}

				if (helper.configuration.file?.sonar?.credentials) {
					def sonarCredentialsId = helper.configuration.file.sonar.credentials
					withCredentials([usernamePassword(
									credentialsId: sonarCredentialsId,
									usernameVariable: 'SONARCLOUD_ORGANIZATION',
									passwordVariable: 'SONARCLOUD_TOKEN'
					)]) {
						sh """ \
								mvn sonar:sonar \
								-Dsonar.organization=${SONARCLOUD_ORGANIZATION} \
								-Dsonar.host.url=https://sonarcloud.io \
								-Dsonar.login=${SONARCLOUD_TOKEN} \
								${helper.scmSource.pullRequest ? """ \
										-Dsonar.pullrequest.branch=${helper.scmSource.branch.name} \
										-Dsonar.pullrequest.key=${helper.scmSource.pullRequest.id} \
										-Dsonar.pullrequest.base=${helper.scmSource.pullRequest.target.name} \
										${helper.scmSource.gitHubRepoId ? """ \
												-Dsonar.pullrequest.provider=GitHub \
												-Dsonar.pullrequest.github.repository=${helper.scmSource.gitHubRepoId} \
										""" : ''} \
								""" : """ \
										-Dsonar.branch.name=${helper.scmSource.branch.name} \
								"""} \
						"""
					}
				}
				else {
					echo "No Sonar organization configured - skipping Sonar report."
				}
			}

			dir(helper.configuration.maven.localRepositoryPath) {
				stash name:'default-build-result', includes:"org/hibernate/search/**"
			}
		}
	}
}

stage('Non-default environments') {
	Map<String, Closure> executions = [:]

	// Test with multiple JDKs
	environments.content.jdk.enabled.each { JdkBuildEnvironment buildEnv ->
		executions.put(buildEnv.tag, {
			runBuildOnNode {
				helper.withMavenWorkspace(jdk: buildEnv.buildJdkTool) {
					// Re-run integration tests against the JARs produced by the default build,
					// but using a different JDK to build and run the tests.
					mavenNonDefaultBuild buildEnv, """ \
							clean install \
					""", 'integrationtest'
				}
			}
		})
	}

	// Build with different compilers
	environments.content.compiler.enabled.each { CompilerBuildEnvironment buildEnv ->
		executions.put(buildEnv.tag, {
			runBuildOnNode {
				helper.withMavenWorkspace {
					mavenNonDefaultBuild buildEnv, """ \
							clean install \
							-DskipTests -DskipITs \
							-P${buildEnv.mavenProfile},!javaModuleITs \
					"""
				}
			}
		})
	}

	// Test ORM integration with multiple databases
	environments.content.database.enabled.each { DatabaseBuildEnvironment buildEnv ->
		executions.put(buildEnv.tag, {
			runBuildOnNode {
				helper.withMavenWorkspace {
					mavenNonDefaultBuild buildEnv, """ \
							clean install \
							-pl org.hibernate.search:hibernate-search-integrationtest-mapper-orm,org.hibernate.search:hibernate-search-integrationtest-mapper-orm-envers,org.hibernate.search:hibernate-search-integrationtest-showcase-library \
							-P$buildEnv.mavenProfile \
					"""
				}
			}
		})
	}

	// Test Elasticsearch integration with multiple versions in a local instance
	environments.content.esLocal.enabled.each { EsLocalBuildEnvironment buildEnv ->
		executions.put(buildEnv.tag, {
			runBuildOnNode {
				helper.withMavenWorkspace {
					mavenNonDefaultBuild buildEnv, """ \
							clean install \
							-pl org.hibernate.search:hibernate-search-integrationtest-backend-elasticsearch,org.hibernate.search:hibernate-search-integrationtest-showcase-library \
							${toElasticsearchVersionArgs(buildEnv.mavenProfile, null)} \
					"""
				}
			}
		})
	}

	// Test Elasticsearch integration with multiple versions in an AWS instance
	environments.content.esAws.enabled.each { EsAwsBuildEnvironment buildEnv ->
		if (!buildEnv.endpointHostAndPort) {
			throw new IllegalStateException("Unexpected empty endpoint host")
		}
		if (!buildEnv.awsRegion) {
			throw new IllegalStateException("Unexpected empty AWS region")
		}
		def awsCredentialsId = null
		if (buildEnv.staticCredentials) {
			awsCredentialsId = helper.configuration.file?.aws?.credentials
			if (!awsCredentialsId) {
				throw new IllegalStateException("Missing AWS credentials")
			}
		}
		executions.put(buildEnv.tag, {
			lock(label: buildEnv.lockedResourcesLabel) {
				runBuildOnNode(NODE_PATTERN_BASE + '&&AWS') {
					if (awsCredentialsId == null) {
						// By default, rely on credentials provided by the EC2 infrastructure

						helper.withMavenWorkspace {
							// Tests may fail because of hourly AWS snapshots,
							// which prevent deleting indexes while they are being executed.
							// Unfortunately, this triggers test failure in @BeforeClass/@AfterClass,
							// which cannot be handled by the maven-failsafe-plugin,
							// which normally re-runs failing tests, but only if
							// the failure occurs in the @Test method.
							// So if this fails, we re-try ALL TESTS, at most twice.
							// Note that because we expect frequent failure and retries,
							// we use --fail-fast here, to make sure we don't waste time.
							retry(count: 3) {
								mavenNonDefaultBuild buildEnv, """ \
										clean install \
										--fail-fast \
										-pl org.hibernate.search:hibernate-search-integrationtest-backend-elasticsearch,org.hibernate.search:hibernate-search-integrationtest-showcase-library \
										${toElasticsearchVersionArgs(buildEnv.mavenProfile, buildEnv.version)} \
										-Dtest.elasticsearch.connection.hosts=$buildEnv.endpointHostAndPort \
										-Dtest.elasticsearch.connection.protocol=$buildEnv.endpointProtocol \
										-Dtest.elasticsearch.connection.aws.signing.enabled=true \
										-Dtest.elasticsearch.connection.aws.region=$buildEnv.awsRegion \
									"""
							}
						}
					}
					else {
						// For a few builds only, rely on static credentials provided by Jenkins
						// (just to check that statically-provided credentials work correctly)

						helper.withMavenWorkspace {
							withCredentials([[$class          : 'AmazonWebServicesCredentialsBinding',
											  credentialsId   : awsCredentialsId,
											  usernameVariable: 'AWS_ACCESS_KEY_ID',
											  passwordVariable: 'AWS_SECRET_ACCESS_KEY'
											 ]]) {
								// Tests may fail because of hourly AWS snapshots,
								// which prevent deleting indexes while they are being executed.
								// Unfortunately, this triggers test failure in @BeforeClass/@AfterClass,
								// which cannot be handled by the maven-failsafe-plugin,
								// which normally re-runs failing tests, but only if
								// the failure occurs in the @Test method.
								// So if this fails, we re-try ALL TESTS, at most twice.
								// Note that because we expect frequent failure and retries,
								// we use --fail-fast here, to make sure we don't waste time.
								retry(count: 3) {
									mavenNonDefaultBuild buildEnv, """ \
										clean install \
										--fail-fast \
										-pl org.hibernate.search:hibernate-search-integrationtest-backend-elasticsearch,org.hibernate.search:hibernate-search-integrationtest-showcase-library \
										${toElasticsearchVersionArgs(buildEnv.mavenProfile, buildEnv.version)} \
										-Dtest.elasticsearch.connection.hosts=$buildEnv.endpointHostAndPort \
										-Dtest.elasticsearch.connection.protocol=$buildEnv.endpointProtocol \
										-Dtest.elasticsearch.connection.aws.signing.enabled=true \
										-Dtest.elasticsearch.connection.aws.region=$buildEnv.awsRegion \
										-Dtest.elasticsearch.connection.aws.credentials.type=static \
										-Dtest.elasticsearch.connection.aws.credentials.access_key_id=$AWS_ACCESS_KEY_ID \
										-Dtest.elasticsearch.connection.aws.credentials.secret_access_key=$AWS_SECRET_ACCESS_KEY \
									"""
								}
							}
						}
					}
				}
			}
		})
	}

	if (executions.isEmpty()) {
		echo 'Skipping builds in non-default environments'
		helper.markStageSkipped()
	}
	else {
		parallel(executions)
	}
}

stage('Deploy') {
	if (deploySnapshot) {
		// TODO delay the release to this stage? This would require to use staging repositories for snapshots, not sure it's possible.
		echo "Already deployed snapshot as part of the 'Default build' stage."
	}
	else if (performRelease) {
		echo "Performing full release for version ${releaseVersion.toString()}"
		runBuildOnNode {
			helper.withMavenWorkspace(mavenSettingsConfig: params.RELEASE_DRY_RUN ? null : helper.configuration.file.deployment.maven.settingsId) {
				sh "git clone https://github.com/hibernate/hibernate-noorm-release-scripts.git"
				sh "bash -xe hibernate-noorm-release-scripts/prepare-release.sh search ${releaseVersion.toString()}"

				String deployCommand = "bash -xe hibernate-noorm-release-scripts/deploy.sh search"
				if (!params.RELEASE_DRY_RUN) {
					sh deployCommand
				} else {
					echo "WARNING: Not deploying. Would have executed:"
					echo deployCommand
				}

				String uploadDistributionCommand = "bash -xe hibernate-noorm-release-scripts/upload-distribution.sh search ${releaseVersion.toString()}"
				String uploadDocumentationCommand = "bash -xe hibernate-noorm-release-scripts/upload-documentation.sh search ${releaseVersion.toString()} ${releaseVersion.family}"
				if (!params.RELEASE_DRY_RUN) {
					sh uploadDistributionCommand
					sh uploadDocumentationCommand
				}
				else {
					echo "WARNING: Not uploading anything. Would have executed:"
					echo uploadDistributionCommand
					echo uploadDocumentationCommand
				}

				sh "bash -xe hibernate-noorm-release-scripts/update-version.sh search ${afterReleaseDevelopmentVersion.toString()}"
				sh "bash -xe hibernate-noorm-release-scripts/push-upstream.sh search ${releaseVersion.toString()} ${helper.scmSource.branch.name} ${!params.RELEASE_DRY_RUN}"
			}
		}
	}
	else {
		echo "Skipping deployment"
		helper.markStageSkipped()
		return
	}
}

} // End of helper.runWithNotification

// Job-specific helpers

enum TestCondition {
	// For environments that are expected to work correctly
	// before merging into master or maintenance branches.
	// Tested on master and maintenance branches, on feature branches, and for PRs.
	BEFORE_MERGE,
	// For environments that are expected to work correctly,
	// but are considered too resource-intensive to test them on pull requests.
	// Tested on master and maintenance branches only.
	// Not tested on feature branches or PRs.
	AFTER_MERGE,
	// For environments that may not work correctly.
	// Only tested when explicitly requested through job parameters.
	ON_DEMAND;

	// Work around JENKINS-33023
	// See https://issues.jenkins-ci.org/browse/JENKINS-33023?focusedCommentId=325738&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-325738
	public TestCondition() {}
}

abstract class BuildEnvironment {
	boolean isDefault = false
	TestCondition condition
	String toString() { getTag() }
	abstract String getTag()
	boolean isDefault() { isDefault }
	boolean requiresDefaultBuildArtifacts() { true }

	String getMavenJdkTool(def allEnvironments) {
		allEnvironments.content.jdk.default.buildJdkTool
	}

	String getElasticsearchJdkTool(def allEnvironments) {
		allEnvironments.content.esLocal.default.jdkTool
	}
}

class JdkBuildEnvironment extends BuildEnvironment {
	String version
	String buildJdkTool
	String testJdkTool
	String testJdkRelease
	@Override
	String getTag() { "jdk-$version" }
	@Override
	String getMavenJdkTool(def allEnvironments) {
		buildJdkTool
	}
}

class CompilerBuildEnvironment extends BuildEnvironment {
	String name
	String mavenProfile
	String getTag() { "compiler-$name" }
	@Override
	boolean requiresDefaultBuildArtifacts() { false }
}

class DatabaseBuildEnvironment extends BuildEnvironment {
	String dbName
	String mavenProfile
	@Override
	String getTag() { "database-$dbName" }
}

class EsLocalBuildEnvironment extends BuildEnvironment {
	String versionRange
	String mavenProfile
	String jdkTool
	@Override
	String getTag() { "elasticsearch-local-$versionRange" }
	@Override
	String getElasticsearchJdkTool(def allEnvironments) {
		jdkTool
	}
}

class EsAwsBuildEnvironment extends BuildEnvironment {
	String version
	String mavenProfile
	String endpointUrl = null
	String endpointHostAndPort = null
	String endpointProtocol = null
	String awsRegion = null
	boolean staticCredentials = false
	@Override
	String getTag() { "elasticsearch-aws-$version" + (staticCredentials ? "-credentials-static" : "") }
	@Override
	String getElasticsearchJdkTool(def allEnvironments) {
		null // No JDK needed for Elasticsearch: the Elasticsearch instance is remote.
	}
	String getNameEmbeddableVersion() {
		version.replaceAll('\\.', '')
	}
	String getEndpointVariableName() {
		"ES_AWS_${nameEmbeddableVersion}_ENDPOINT"
	}
	String getLockedResourcesLabel() {
		"es-aws-${nameEmbeddableVersion}"
	}
	String setEndpointUrl(String url) {
		this.endpointUrl = url
		if ( endpointUrl ) {
			def matcher = endpointUrl =~ /^(?:(https?):\/\/)?(.*)$/
			endpointProtocol = matcher[0][1]
			endpointHostAndPort = matcher[0][2]
		}
		else {
			endpointProtocol = null
			endpointHostAndPort = null
		}
	}
}

void keepOnlyEnvironmentsMatchingFilter(String regex) {
	def pattern = /$regex/

	boolean enableDefault = ('default' =~ pattern)

	environments.content.each { key, envSet ->
		envSet.enabled.removeAll { buildEnv ->
			!(buildEnv.tag =~ pattern) && !(envSet.default == buildEnv && enableDefault)
		}
	}
}

void keepOnlyEnvironmentsFromSet(String environmentSetName) {
	boolean enableDefaultEnv = false
	boolean enableBeforeMergeEnvs = false
	boolean enableAfterMergeEnvs = false
	boolean enableOnDemandEnvs = false
	switch (environmentSetName) {
		case 'DEFAULT':
			enableDefaultEnv = true
			break
		case 'SUPPORTED':
			enableDefaultEnv = true
			enableBeforeMergeEnvs = true
			enableAfterMergeEnvs = true
			break
		case 'ALL':
			enableDefaultEnv = true
			enableBeforeMergeEnvs = true
			enableAfterMergeEnvs = true
			enableOptional = true
			break
		case 'AUTOMATIC':
			if (params.RELEASE_VERSION) {
				echo "Releasing version '$params.RELEASE_VERSION'."
			} else if (helper.scmSource.pullRequest) {
				echo "Building pull request '$helper.scmSource.pullRequest.id'"
				enableDefaultEnv = true
				enableBeforeMergeEnvs = true
			} else if (helper.scmSource.branch.primary) {
				echo "Building primary branch '$helper.scmSource.branch.name'"
				enableDefaultEnv = true
				enableBeforeMergeEnvs = true
				enableAfterMergeEnvs = true
			} else {
				echo "Building feature branch '$helper.scmSource.branch.name'"
				enableDefaultEnv = true
				enableBeforeMergeEnvs = true
			}
			break
		default:
			throw new IllegalArgumentException(
					"Unknown value for param 'ENVIRONMENT_SET': '$environmentSetName'."
			)
	}

	// Filter environments

	environments.content.each { key, envSet ->
		envSet.enabled.removeAll { buildEnv -> ! (
				enableDefaultEnv && buildEnv.isDefault ||
				enableBeforeMergeEnvs && buildEnv.condition == TestCondition.BEFORE_MERGE ||
				enableAfterMergeEnvs && buildEnv.condition == TestCondition.AFTER_MERGE ||
						enableOnDemandEnvs && buildEnv.condition == TestCondition.ON_DEMAND ) }
	}
}

void runBuildOnNode(Closure body) {
	runBuildOnNode( NODE_PATTERN_BASE, body )
}

void runBuildOnNode(String label, Closure body) {
	node( label ) {
		timeout( [time: 1, unit: 'HOURS'], body )
	}
}

void mavenNonDefaultBuild(BuildEnvironment buildEnv, String args, String projectPath = '.') {
	if ( buildEnv.requiresDefaultBuildArtifacts() ) {
		dir(helper.configuration.maven.localRepositoryPath) {
			unstash name:'default-build-result'
		}
	}

	// Add a suffix to tests to distinguish between different executions
	// of the same test in different environments in reports
	def testSuffix = buildEnv.tag.replaceAll('[^a-zA-Z0-9_\\-+]+', '_')

	dir(projectPath) {
		sh """ \
				mvn -Dsurefire.environment=$testSuffix \
						${toTestJdkArg(buildEnv)} \
						${toElasticsearchJdkArg(buildEnv)} \
						--fail-at-end \
						$args \
		"""
	}
}

String toElasticsearchVersionArgs(String mavenEsProfile, String version) {
	String defaultEsProfile = environments.content.esLocal.default.mavenProfile
	if ( version ) {
		// The default profile is disabled, because a version is passed explicitly
		// We just need to set the correct profile and pass the version
		"-P$mavenEsProfile -Dtest.elasticsearch.connection.version=$version"
	}
	else if ( mavenEsProfile != defaultEsProfile) {
		// Disable the default profile to avoid conflicting configurations
		"-P!$defaultEsProfile,$mavenEsProfile"
	}
	else {
		// Do not do as above, as we would tell Maven "disable the default profile, but enable it"
		// and Maven would end up disabling it.
		''
	}
}

String toTestJdkArg(BuildEnvironment buildEnv) {
	String args = ''

	if ( ! (buildEnv instanceof JdkBuildEnvironment) ) {
		return args;
	}

	String testJdkTool = buildEnv.testJdkTool
	if ( testJdkTool ) {
		def testJdkToolPath = tool(name: testJdkTool, type: 'jdk')
		args += " -Dsurefire.jvm.java_home=$testJdkToolPath"
	}
	String testJdkRelease = buildEnv.testJdkRelease
	if ( testJdkRelease ) {
		args += " -Dmaven.compiler.testRelease=$testJdkRelease"
	}

	return args
}

String toElasticsearchJdkArg(BuildEnvironment buildEnv) {
	String elasticsearchJdkTool = buildEnv.getElasticsearchJdkTool(environments)

	if (elasticsearchJdkTool == null || buildEnv.getMavenJdkTool(environments) == elasticsearchJdkTool) {
		return '' // No specific JDK needed
	}

	def elasticsearchJdkToolPath = tool(name: elasticsearchJdkTool, type: 'jdk')
	return "-Dtest.elasticsearch.run.java_home=$elasticsearchJdkToolPath"
}
