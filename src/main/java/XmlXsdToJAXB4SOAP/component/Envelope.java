package XmlXsdToJAXB4SOAP.component;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;
import java.math.BigDecimal;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Envelope {
    @XmlElement(name="Header", namespace="http://schemas.xmlsoap.org/soap/envelope/")
    private String header;
    @XmlAccessorType(XmlAccessType.FIELD)
    @Data
    public static class Body {
        @XmlAccessorType(XmlAccessType.FIELD)
        @Data
        public static class FaturaOdemeRequest {
            @XmlElement(name="mesajTipi", namespace="http://ortak.model.server.webservice.business.tts.innova.com.tr")
            private Long mesajtipi;
            @XmlElement(name="islemKodu", namespace="http://ortak.model.server.webservice.business.tts.innova.com.tr")
            private Long islemkodu;
            @XmlElement(name="stan", namespace="http://ortak.model.server.webservice.business.tts.innova.com.tr")
            private Long stan;
            @XmlElement(name="kurumKodu", namespace="http://ortak.model.server.webservice.business.tts.innova.com.tr")
            private Long kurumkodu;
            @XmlAccessorType(XmlAccessType.FIELD)
            @Data
            public static class IslemYapan {
                @XmlElement(name="sehirKodu", namespace="http://ortak.model.server.webservice.business.tts.innova.com.tr")
                private Long sehirkodu;
                @XmlElement(name="subeKodu", namespace="http://ortak.model.server.webservice.business.tts.innova.com.tr")
                private Long subekodu;
                @XmlElement(name="giseKodu", namespace="http://ortak.model.server.webservice.business.tts.innova.com.tr")
                private Long gisekodu;
                @XmlElement(name="kullaniciKodu", namespace="http://ortak.model.server.webservice.business.tts.innova.com.tr")
                private Long kullanicikodu;
            }
            @XmlElement(name="sirketKodu", namespace="http://ortak.model.server.webservice.business.tts.innova.com.tr")
            private Long sirketkodu;
            @XmlElement(name="paraKodu", namespace="http://ortak.model.server.webservice.business.tts.innova.com.tr")
            private Long parakodu;
            @XmlElement(name="islemKaynagi", namespace="http://ortak.model.server.webservice.business.tts.innova.com.tr")
            private Long islemkaynagi;
            @XmlElement(name="islemTarihi", namespace="http://ortak.model.server.webservice.business.tts.innova.com.tr")
            private Long islemtarihi;
            @XmlElement(name="islemSaati", namespace="http://ortak.model.server.webservice.business.tts.innova.com.tr")
            private Long islemsaati;
            @XmlElement(name="mesajTarihSaat", namespace="http://banka.model.server.webservice.business.tts.innova.com.tr")
            private Long mesajtarihsaat;
            @XmlElement(name="kayitSayisi", namespace="http://banka.model.server.webservice.business.tts.innova.com.tr")
            private Long kayitsayisi;
            @XmlElement(name="islemKabulTarihi", namespace="http://banka.model.server.webservice.business.tts.innova.com.tr")
            private Long islemkabultarihi;
            @XmlAccessorType(XmlAccessType.FIELD)
            @Data
            public static class FaturaBilgiDizi {
                @XmlAccessorType(XmlAccessType.FIELD)
                @Data
                public static class FaturaBilgi {
                    @XmlElement(name="hesapNo", namespace="null")
                    private Long hesapno;
                    @XmlElement(name="toplamBorcTutari", namespace="null")
                    private Long toplamborctutari;
                    @XmlElement(name="referansNo", namespace="null")
                    private Long referansno;
                    @XmlElement(name="faturaNo", namespace="null")
                    private Long faturano;
                    @XmlElement(name="faturaTaksitNo", namespace="null")
                    private Long faturataksitno;
                    @XmlElement(name="odemeDonemi", namespace="null")
                    private Long odemedonemi;
                    @XmlElement(name="hesapId", namespace="null")
                    private Long hesapıd;
                    @XmlAccessorType(XmlAccessType.FIELD)
                    @Data
                    public static class ReferansBilgi {
                        @XmlElement(name="stan", namespace="http://banka.model.server.webservice.business.tts.innova.com.tr")
                        private Long stan;
                        @XmlElement(name="islemTarihi", namespace="http://banka.model.server.webservice.business.tts.innova.com.tr")
                        private Long islemtarihi;
                    }
                }
            }
        }
    }
}
