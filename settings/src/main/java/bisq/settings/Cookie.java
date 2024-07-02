/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.settings;

import bisq.common.proto.PersistableProto;
import bisq.settings.protobuf.CookieMapEntry;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Serves as flexible container for persisting UI states, layout,...
 * Should not be over-used for domain specific data where type safety and data integrity is important.
 * Does not support observable properties.
 */
public final class Cookie implements PersistableProto {
    private final Map<CookieMapKey, String> map = new ConcurrentHashMap<>();

    public Cookie() {
    }

    private Cookie(Map<CookieMapKey, String> map) {
        this.map.putAll(map);
    }

    @Override
    public bisq.settings.protobuf.Cookie.Builder getBuilder(boolean serializeForHash) {
        return bisq.settings.protobuf.Cookie.newBuilder()
                .addAllCookieMapEntries(map.entrySet().stream()
                        .map(entry -> {
                            CookieMapKey cookieMapKey = entry.getKey();
                            CookieMapEntry.Builder builder = CookieMapEntry.newBuilder()
                                    .setKey(cookieMapKey.getCookieKey().name())
                                    .setValue(entry.getValue());
                            cookieMapKey.getSubKey().ifPresent(builder::setSubKey);
                            return builder.build();
                        })
                        .collect(Collectors.toList()));
    }

    @Override
    public bisq.settings.protobuf.Cookie toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    static Cookie fromProto(bisq.settings.protobuf.Cookie proto) {
        return new Cookie(proto.getCookieMapEntriesList().stream()
                .collect(Collectors.toMap(
                        entry -> {
                            Optional<String> subKey = entry.hasSubKey() ? Optional.of(entry.getSubKey()) : Optional.empty();
                            return CookieMapKey.fromProto(entry.getKey(), subKey);
                        },
                        CookieMapEntry::getValue)));
    }

    public Optional<String> asString(CookieKey key) {
        return asString(key, null);
    }

    public Optional<String> asString(CookieKey key, @Nullable String subKey) {
        return Optional.ofNullable(map.get(new CookieMapKey(key, subKey)));
    }

    public Optional<Double> asDouble(CookieKey key) {
        return asString(key)
                .flatMap(stringValue -> {
                    try {
                        return Optional.of(Double.parseDouble(stringValue));
                    } catch (Throwable t) {
                        return Optional.empty();
                    }
                }).stream().findAny();
    }

    public Optional<Double> asDouble(CookieKey key, String subKey) {
        return asString(key, subKey)
                .flatMap(stringValue -> {
                    try {
                        return Optional.of(Double.parseDouble(stringValue));
                    } catch (Throwable t) {
                        return Optional.empty();
                    }
                }).stream().findAny();
    }

    public Optional<Boolean> asBoolean(CookieKey key) {
        return asString(key).map(stringValue -> stringValue.equals("1"));
    }

    public Optional<Boolean> asBoolean(CookieKey key, String subKey) {
        return asString(key, subKey)
                .map(stringValue -> stringValue.equals("1"));
    }

    void putAsString(CookieKey key, String value) {
        putAsString(key, null, value);
    }

    void putAsString(CookieKey key, @Nullable String subKey, String value) {
        map.put(new CookieMapKey(key, subKey), value);
    }

    void putAll(Map<CookieMapKey, String> map) {
        map.forEach((key, value) -> putAsString(key.getCookieKey(), value));
    }

    void putAsBoolean(CookieKey key, boolean value) {
        putAsString(key, value ? "1" : "0");
    }

    void putAsDouble(CookieKey key, double value) {
        putAsString(key, String.valueOf(value));
    }

    void remove(CookieKey key) {
        remove(key, null);
    }

    void remove(CookieKey key, @Nullable String subKey) {
        map.remove(new CookieMapKey(key, subKey));
    }

    Map<CookieMapKey, String> getMap() {
        return map;
    }
}
