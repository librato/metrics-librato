package com.librato.metrics.reporter;

import com.librato.metrics.client.Tag;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Collections;
import java.util.List;

public class Signal {
    public String name;
    public String source;
    public List<Tag> tags = Collections.emptyList();
    public boolean overrideTags;

    public static Signal decode(String data) {
        if (data == null || data.length() < 2 || data.charAt(0) != '{') {
            return new Signal(data);
        }
        return Json.decode(data, Signal.class);
    }

    public Signal(String name) {
        this.name = name;
    }

    public Signal(String name, String source) {
        this.name = name;
        this.source = source;
    }

    @JsonCreator
    public Signal(@JsonProperty("name") String name,
                  @JsonProperty("source") String source,
                  @JsonProperty("tags") List<Tag> tags,
                  @JsonProperty("overrideTags") boolean overrideTags) {
        this.name = name;
        this.source = source;
        if (tags != null) {
            this.tags = tags;
        }
        this.overrideTags = overrideTags;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Signal signal = (Signal) o;

        if (overrideTags != signal.overrideTags) return false;
        if (name != null ? !name.equals(signal.name) : signal.name != null)
            return false;
        if (source != null ? !source.equals(signal.source) : signal.source != null)
            return false;
        return tags != null ? tags.equals(signal.tags) : signal.tags == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        result = 31 * result + (overrideTags ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Signal{");
        sb.append("name='").append(name).append('\'');
        sb.append(", source='").append(source).append('\'');
        sb.append(", tags=").append(tags);
        sb.append(", overrideTags=").append(overrideTags);
        sb.append('}');
        return sb.toString();
    }
}
