package naef.dto;

public class JackDto extends HardwareDto {

    public static class ExtAttr {

        public static final SingleRefAttr<PortDto, JackDto> PORT
            = new SingleRefAttr<PortDto, JackDto>("port");
    }

    public JackDto() {
    }

    public PortDto getPort() {
        return ExtAttr.PORT.deref(this);
    }
}
