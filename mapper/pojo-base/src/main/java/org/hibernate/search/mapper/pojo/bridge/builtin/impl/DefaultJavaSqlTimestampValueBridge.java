/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.sql.Timestamp;
import java.time.Instant;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public final class DefaultJavaSqlTimestampValueBridge implements ValueBridge<Timestamp, Instant> {

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Instant toIndexedValue(Timestamp value, ValueBridgeToIndexedValueContext context) {
		return value == null ? null : Instant.ofEpochMilli( value.getTime() );
	}

	@Override
	public Timestamp fromIndexedValue(Instant value, ValueBridgeFromIndexedValueContext context) {
		return value == null ? null : new Timestamp( value.toEpochMilli() );
	}

	@Override
	public Instant parse(String value) {
		return ParseUtils.parseInstant( value );
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() );
	}

}