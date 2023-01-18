import java.util.logging.Level;
import java.util.logging.Logger;
class Main {

        public static final String AUSTIN_POWERS = "Austin Powers";
        public static final String WEAPONS = "weapons";
        public static final String BANNED_SUBSTANCE = "banned substance";

        /*
        Something changed here just for a TEST
        */

        static class Spy implements MailService {
            private final static Logger LOGGER = Logger.getLogger(Spy.class.getName());
            private Logger logger;

            public Spy(Logger logger) {
                this.logger = logger;
            }

            @Override
            public Sendable processMail(Sendable mail) {
                if (mail instanceof MailMessage) {
                    if (mail.getFrom().contains(AUSTIN_POWERS) || mail.getTo().contains(AUSTIN_POWERS)) {
                        logger.log(Level.WARNING,
                                "Detected target mail correspondence: from {0} to {1} \"{2}\"",
                                new Object[]{mail.getFrom(), mail.getTo(), ((MailMessage) mail).getMessage()});
                    } else {
                        logger.log(Level.INFO,
                                "Usual correspondence: from {0} to {1}",
                                new Object[]{mail.getFrom(), mail.getTo()});
                    }
                }
                return mail;
            }
        }

        static class Inspector implements MailService {
            @Override
            public Sendable processMail(Sendable mail) {
                MailPackage mailPackage;
                if (mail instanceof MailPackage) {
                    mailPackage = (MailPackage) mail;
                    if (mailPackage.getContent().getContent().contains(WEAPONS) || mailPackage.getContent().getContent().contains(BANNED_SUBSTANCE)) {
                        throw new IllegalPackageException();
                    }
                    if (mailPackage.getContent().getContent().contains("stones")) {
                        throw new StolenPackageException();
                    }
                }
                return mail;
            }
        }

        static class Thief implements MailService {
            private int minPrice;
            private static int stolenValue = 0;

            public Thief(int minPrice) {
                this.minPrice = minPrice;
            }

            public int getStolenValue() {
                return stolenValue;
            }

            @Override
            public Sendable processMail(Sendable mail) {
                if (mail instanceof MailPackage) {
                    MailPackage pack;
                    pack = (MailPackage) mail;
                    if (pack.getContent().getPrice() >= minPrice) {
                        stolenValue += pack.getContent().getPrice();
                        return new MailPackage(pack.getFrom(), pack.getTo(), new Package("stones instead of " + pack.getContent().getContent(), 0));
                    }
                }
                return mail;
            }
        }

        static class UntrustworthyMailWorker implements MailService {
            private RealMailService realMailService;
            private MailService[] mailServices;

            public UntrustworthyMailWorker(MailService[] mailServices) {
                realMailService = new RealMailService("realMailService");
                this.mailServices = mailServices;
            }

            public MailService getRealMailService() {
                return realMailService;
            }

            @Override
            public Sendable processMail(Sendable mail) {
                Sendable sendable = mail;
                for (MailService service : mailServices) {
                    sendable = service.processMail(sendable);
                }
                return getRealMailService().processMail(sendable);
            }
        }

        static class StolenPackageException extends RuntimeException {

        }

        static class IllegalPackageException extends RuntimeException {

        }

        //Stepik code: end

        public static void main(String[] args) {
            Logger logger = Logger.getLogger(Main.class.getName());
            Inspector inspector = new Inspector();
            Spy spy = new Spy(logger);
            Thief thief = new Thief(10000);
            UntrustworthyMailWorker worker = new UntrustworthyMailWorker(new MailService[]{spy, thief, inspector});
            AbstractSendable[] correspondence = {
                    new MailMessage("Oxxxymiron", "Гнойный", " Я здесь чисто по фану, поглумиться над слабым\n" +
                            "Ты же вылез из мамы под мой дисс на Бабана...."),
                    new MailMessage("Гнойный", "Oxxxymiron", "....Что? Так болел за Россию, что на нервах терял ганглии.\n" +
                            "Но когда тут проходили митинги, где ты сидел? В Англии!...."),
                    new MailMessage("Жириновский", AUSTIN_POWERS, "Бери пацанов, и несите меня к воде."),
                    new MailMessage(AUSTIN_POWERS, "Пацаны", "Го, потаскаем Вольфовича как Клеопатру"),
                    new MailPackage("берег", "море", new Package("ВВЖ", 32)),
                    new MailMessage("NASA", AUSTIN_POWERS, "Найди в России ракетные двигатели и лунные stones"),
                    new MailPackage(AUSTIN_POWERS, "NASA", new Package("ракетный двигатель ", 2500000)),
                    new MailPackage(AUSTIN_POWERS, "NASA", new Package("stones ", 1000)),
                    new MailPackage("Китай", "КНДР", new Package("banned substance ", 10000)),
                    new MailPackage(AUSTIN_POWERS, "Жопа запрещенная группировка", new Package("tiny bomb", 9000)),
                    new MailMessage(AUSTIN_POWERS, "Психиатр", "Помогите"),
            };

            for (AbstractSendable p : correspondence) {
                try {
                    print("До:  ", p);
                    Sendable sendable = worker.processMail(p);
                    print("После:  ", sendable);
                } catch (StolenPackageException | IllegalPackageException e) {
                    logger.log(Level.WARNING, "из: " + p.getFrom() + " куда: " + p.getTo() + " Содержимое: "
                            + (p instanceof MailMessage ? ((MailMessage) p).getMessage() : ((MailPackage) p).getContent().getContent()
                            + " Цена=" + ((MailPackage) p).getContent().getPrice()) + " Exceptions: " + e);
                }
            }
        }

        public static void print(String prefix, Sendable p) {
            System.out.println(prefix + "из: " + p.getFrom() + " куда: " + p.getTo() + " Содержимое: "
                    + (p instanceof MailMessage ? ((MailMessage) p).getMessage() : ((MailPackage) p).getContent().getContent()
                    + " Цена=" + ((MailPackage) p).getContent().getPrice()));
        }
        /*
            Интерфейс: сущность, которую можно отправить по почте.
            У такой сущности можно получить от кого и кому направляется письмо.
        */
        public static interface Sendable {
            String getFrom();
            String getTo();
        }
        /*
           Абстрактный класс,который позволяет абстрагировать логику хранения
           источника и получателя письма в соответствующих полях класса.
        */
        public static abstract class AbstractSendable implements Sendable {
            protected final String from;
            protected final String to;

            public AbstractSendable(String from, String to) {
                this.from = from;
                this.to = to;
            }

            @Override
            public String getFrom() {
                return from;
            }

            @Override
            public String getTo() {
                return to;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                AbstractSendable that = (AbstractSendable) o;
                if (!from.equals(that.from)) return false;
                if (!to.equals(that.to)) return false;
                return true;
            }
        }
        /*
        Письмо, у которого есть текст, который можно получить с помощью метода `getMessage`
        */
        public static class MailMessage extends AbstractSendable {
            private final String message;

            public MailMessage(String from, String to, String message) {
                super(from, to);
                this.message = message;
            }

            public String getMessage() {
                return message;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                if (!super.equals(o)) return false;
                MailMessage that = (MailMessage) o;
                if (message != null ? !message.equals(that.message) : that.message != null) return false;
                return true;
            }
        }
        /*
        Посылка, содержимое которой можно получить с помощью метода `getContent`
        */
        public static class MailPackage extends AbstractSendable {
            private final Package content;

            public MailPackage(String from, String to, Package content) {
                super(from, to);
                this.content = content;
            }

            public Package getContent() {
                return content;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                if (!super.equals(o)) return false;
                MailPackage that = (MailPackage) o;
                if (!content.equals(that.content)) return false;
                return true;
            }
        }
        /*
        Класс, который задает посылку. У посылки есть текстовое описание содержимого и целочисленная ценность.
        */
        public static class Package {
            private final String content;
            private final int price;

            public Package(String content, int price) {
                this.content = content;
                this.price = price;
            }

            public String getContent() {
                return content;
            }

            public int getPrice() {
                return price;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Package aPackage = (Package) o;

                if (price != aPackage.price) return false;
                if (!content.equals(aPackage.content)) return false;

                return true;
            }
        }

        /*
        Интерфейс, который задает класс, который может каким-либо образом обработать почтовый объект.
        */
        public static interface MailService {
            Sendable processMail(Sendable mail);
        }

        /*
        Класс, в котором скрыта логика настоящей почты
        */
        public static class RealMailService implements MailService {

            public RealMailService(String realMailService) {

            }

            @Override
            public Sendable processMail(Sendable mail) {
                // Здесь описан код настоящей системы отправки почты.
                return mail;
            }
        }
}