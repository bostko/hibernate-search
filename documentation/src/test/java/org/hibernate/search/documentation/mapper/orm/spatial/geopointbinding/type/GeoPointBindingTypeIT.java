/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.spatial.geopointbinding.type;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingSynchronizationStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class GeoPointBindingTypeIT {

	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendConfigurations() {
		return BackendConfigurations.simple().toArray();
	}

	@Rule
	public OrmSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	public GeoPointBindingTypeIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = OrmSetupHelper.withSingleBackend( backendConfiguration );
	}

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						AutomaticIndexingSynchronizationStrategyName.SEARCHABLE
				)
				.setup( Author.class );
	}

	@Test
	public void smoke() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			Author author = new Author();
			author.setName( "Isaac Asimov" );
			author.setPlaceOfBirthLatitude( 53.976177 );
			author.setPlaceOfBirthLongitude( 32.158627 );

			entityManager.persist( author );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Author> result = searchSession.search( Author.class )
					.predicate( f -> f.spatial().within().onField( "placeOfBirth" )
							.circle( 53.970000, 32.150000, 50, DistanceUnit.KILOMETERS ) )
					.fetchHits();
			assertThat( result ).hasSize( 1 );
		} );
	}

}