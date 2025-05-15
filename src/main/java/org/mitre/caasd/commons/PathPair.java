package org.mitre.caasd.commons;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public record PathPair(VehiclePath path0, VehiclePath path1) {

    public PathPair {
        requireNonNull(path0);
        requireNonNull(path1);
        checkArgument(path0.size() == path1.size());
    }
}
