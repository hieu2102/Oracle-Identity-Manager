package vn.bnh.oim.scheduledtasks.users;

import oracle.iam.connectors.icfcommon.recon.SearchReconDeleteTask;
import oracle.iam.identity.exception.SearchKeyNotUniqueException;
import oracle.iam.identity.exception.UserManagerException;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.scheduler.vo.TaskSupport;
import vn.bnh.oim.utils.NotificationUtils;
import vn.bnh.oim.utils.ReconciliationUtils;
import vn.bnh.oim.utils.UserUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class PostUserReconciliationTask extends TaskSupport {
    private String resourceObjectName;
    private HashMap<String, Object> params;
    private final String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());

    @Override
    public void execute(HashMap params) throws Exception {
        this.params = params;
        setAttributes();
        Set<User> userList = ReconciliationUtils.getReconciledUsers(this.resourceObjectName, currentDate);
        userList = userList.stream().filter(user -> user.getPasswordCreationDate() == null).collect(Collectors.toSet());
        userList.forEach(user -> {
            try {
                user.getPasswordGenerated();
                String userPassword = UserUtils.setUserPassword(user.getLogin());
                NotificationUtils.sendUserCreatedNotification(user, userPassword);
            } catch (UserManagerException | SearchKeyNotUniqueException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public HashMap getAttributes() {
        return null;
    }

    @Override
    public void setAttributes() {
        this.resourceObjectName = this.params.get("Resource Object Name").toString();
    }
}
