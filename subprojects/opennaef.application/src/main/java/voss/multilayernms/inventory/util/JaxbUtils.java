package voss.multilayernms.inventory.util;

import org.xml.sax.SAXException;
import voss.multilayernms.inventory.exception.XmlException;

import javax.xml.bind.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class JaxbUtils {
    private JaxbUtils() {
    }

    public static Object unmarshall(final Class<?> clazz, final URL url) throws XmlException, IOException {
        InputStream is = null;
        try {
            is = url.openStream();
            return unmarshall(clazz, is);
        } finally {
            if (null != is) {
                is.close();
            }
        }
    }

    public static Object unmarshall(final Class<?> clazz, final File file) throws XmlException, IOException {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            return unmarshall(clazz, is);
        } finally {
            if (null != is) {
                is.close();
            }
        }
    }

    public static Object unmarshall(final Class<?> clazz, final InputStream is) throws XmlException {
        try {
            JAXBContext jc = JAXBContext.newInstance(clazz.getPackage().getName());
            Unmarshaller u = jc.createUnmarshaller();
            SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            Schema schema = factory.newSchema(new File("schema/" + clazz.getSimpleName() + ".xsd"));
            u.setSchema(schema);
            u.setEventHandler(new ValidationEventHandler() {
                public boolean handleEvent(final ValidationEvent event) {
                    return false;
                }
            });
            return u.unmarshal(is);
        } catch (JAXBException ex) {
            throw new XmlException(ex);
        } catch (SAXException ex) {
            throw new XmlException(ex);
        }
    }

    public static void marshall(final Object obj, final Writer writer) throws XmlException {
        try {
            JAXBContext jc = JAXBContext.newInstance(obj.getClass().getPackage().getName());
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
            m.setProperty("jaxb.encoding", "Windows-31j");
            m.marshal(obj, writer);
        } catch (JAXBException ex) {
            throw new XmlException(ex);
        }
    }

    public static class InetAddressAdapter extends XmlAdapter<String, InetAddress> {
        public String marshal(final InetAddress address) {
            return address.getHostName();
        }

        public InetAddress unmarshal(final String str) throws UnknownHostException {
            return InetAddress.getByName(str);
        }
    }

    public static class DateAdapter extends XmlAdapter<String, Date> {
        private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");

        public String marshal(final Date date) {
            return sdf.format(date);
        }

        public Date unmarshal(final String str) throws ParseException {
            if (null == str || "".equals(str)) {
                return null;
            }
            return sdf.parse(str);
        }
    }

    public static class TimeAdapter extends XmlAdapter<String, Date> {
        private SimpleDateFormat sdf = new SimpleDateFormat("hh:mm");

        public String marshal(final Date date) {
            return sdf.format(date);
        }

        public Date unmarshal(final String str) throws ParseException {
            if (null == str || "".equals(str)) {
                return null;
            }
            return sdf.parse(str);
        }
    }
}