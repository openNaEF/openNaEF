package naef.dto.isdn;

import naef.dto.HardPortDto;

import java.util.Set;

public class IsdnPortDto extends HardPortDto {

    public static class ExtAttr {

        public static final SetRefAttr<IsdnChannelIfDto, IsdnPortDto> CHANNELS
            = new SetRefAttr<IsdnChannelIfDto, IsdnPortDto>("Channels");
    }

    public IsdnPortDto() {
    }

    public Set<IsdnChannelIfDto> getChannels() {
        return ExtAttr.CHANNELS.deref(this);
    }
}
