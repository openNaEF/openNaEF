package voss.multilayernms.inventory.scheduler;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.MplsNmsLogCategory;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.UrlUtil;

import java.io.Serializable;

public class SchedulerTaskPage extends WebPage {

    public static final String OPERATION_NAME = "SchedulerTask";

    private final String editorName;
    private final RttJobModel rttJobModel;
    private final OperStatusJobModel operStatusJobModel;

    public SchedulerTaskPage() {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.rttJobModel = new RttJobModel();
            this.rttJobModel.isRunning();
            this.operStatusJobModel = new OperStatusJobModel();
            this.operStatusJobModel.isRunning();

            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            BookmarkablePageLink<Void> refresh = new BookmarkablePageLink<Void>("refresh", SchedulerTaskPage.class);
            add(refresh);

            final SchedulerModel serviceModel = new SchedulerModel();
            Label schedulerStatus = new Label("schedulerStatus", new PropertyModel<String>(serviceModel, "status"));
            add(schedulerStatus);

            Form<Void> schedulerForm = new Form<Void>("scheduler");
            add(schedulerForm);
            SubmitLink startService = new SubmitLink("startService") {
                private static final long serialVersionUID = 1L;

                public void onSubmit() {
                    SchedulerService service = SchedulerService.getInstance();
                    service.startService();
                    Logger logAAA = LoggerFactory.getLogger(MplsNmsLogCategory.LOG_AAA);
                    logAAA.info("start scheduler by " + editorName);
                }
            };
            schedulerForm.add(startService);
            SubmitLink stopService = new SubmitLink("stopService") {
                private static final long serialVersionUID = 1L;

                public void onSubmit() {
                    SchedulerService service = SchedulerService.getInstance();
                    service.stopService();
                    Logger logAAA = LoggerFactory.getLogger(MplsNmsLogCategory.LOG_AAA);
                    logAAA.info("stop scheduler by " + editorName);
                }
            };
            schedulerForm.add(stopService);

            final Label rttStatusLabel = new Label("rttStatus", new PropertyModel<String>(this.rttJobModel, "status"));
            add(rttStatusLabel);
            final Label goStopRttJobCaption = new Label("caption",
                    new PropertyModel<String>(this.rttJobModel, "operationCaption"));
            Link<Void> goStopRttJob = new Link<Void>("goStopRttJob") {
                private static final long serialVersionUID = 1L;

                public void onClick() {
                    if (rttJobModel.isRunning()) {
                        rttJobModel.stop();
                    } else {
                        rttJobModel.start();
                    }
                    goStopRttJobCaption.modelChanged();
                    rttStatusLabel.modelChanged();
                }
            };
            add(goStopRttJob);
            goStopRttJob.add(goStopRttJobCaption);

            final Label oprtStatusLabel = new Label("operStatus", new PropertyModel<String>(this.operStatusJobModel, "status"));
            add(oprtStatusLabel);
            final Label goStopOperStatusJobCaption = new Label("caption", new PropertyModel<String>(this.operStatusJobModel, "operationCaption"));
            Link<Void> goStopOperStatusJob = new Link<Void>("goStopOperStatusJob") {
                private static final long serialVersionUID = 1L;

                public void onClick() {
                    if (operStatusJobModel.isRunning()) {
                        operStatusJobModel.stop();
                    } else {
                        operStatusJobModel.start();
                    }
                    goStopOperStatusJobCaption.modelChanged();
                    oprtStatusLabel.modelChanged();
                }
            };
            goStopOperStatusJob.add(goStopOperStatusJobCaption);
            add(goStopOperStatusJob);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private class SchedulerModel implements Serializable {
        private static final long serialVersionUID = 1L;

        public String getStatus() {
            return SchedulerService.getInstance().isServicing() ? "In Service" : "Not In Service";
        }
    }

    private class RttJobModel implements Serializable {
        private static final long serialVersionUID = 1L;
        private boolean isRunning = false;

        public ScheduledJob getJob() {
            return SchedulerService.getInstance().getRttJob();
        }

        public void start() {
            if (getJob() == null) {
                return;
            }
            getJob().begin();
        }

        public void stop() {
            if (getJob() == null) {
                return;
            }
            getJob().end();
        }

        public boolean isRunning() {
            if (getJob() == null) {
                this.isRunning = false;
            } else {
                this.isRunning = getJob().isRunning();
            }
            return this.isRunning;
        }

        public String getOperationCaption() {
            if (this.isRunning) {
                return "Abort";
            } else {
                return "Start";
            }
        }

        public String getStatus() {
            if (this.isRunning) {
                return "Running";
            } else {
                return "Idle";
            }
        }

        public String getLastExecutionTime() {
            return "1900/01/01 00:00:00";
        }

    }

    private class OperStatusJobModel implements Serializable {
        private static final long serialVersionUID = 1L;
        private boolean isRunning = false;

        public ScheduledJob getJob() {
            return SchedulerService.getInstance().getOperStatusJob();
        }

        public void start() {
            if (getJob() == null) {
                return;
            }
            getJob().begin();
        }

        public void stop() {
            if (getJob() == null) {
                return;
            }
            getJob().end();
        }

        public boolean isRunning() {
            if (getJob() == null) {
                this.isRunning = false;
            } else {
                this.isRunning = getJob().isRunning();
            }
            return this.isRunning;
        }

        public String getOperationCaption() {
            if (this.isRunning) {
                return "Abort";
            } else {
                return "Start";
            }
        }

        public String getStatus() {
            if (this.isRunning) {
                return "Running";
            } else {
                return "Idle";
            }
        }

        public String getLastExecutionTime() {
            return "1900/01/01 00:00:00";
        }
    }

}