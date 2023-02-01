package band.gosrock.api.common.aop.hostRole;


import lombok.Getter;

@Getter
public enum FindHostFrom {
    HOST_ID("hostId"),
    EVENT_ID("eventId");

    private final String identifier;

    FindHostFrom(String identifier) {
        this.identifier = identifier;
    }
}
