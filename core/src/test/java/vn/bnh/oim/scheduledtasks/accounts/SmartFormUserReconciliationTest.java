package vn.bnh.oim.scheduledtasks.accounts;

import com.fasterxml.jackson.core.JsonProcessingException;
import oracle.iam.connectors.icfcommon.recon.SearchReconTask;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.junit.Test;
import vn.bnh.oim.utils.OIMUtils;
import vn.bnh.oim.utils.SchedulerServiceUtils;
import vn.bnh.oim.utils.ServerUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SmartFormUserReconciliationTest {
    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String password = "oracle_4U";

    @Test
    public void abstractRecon() throws Exception {
        OIMUtils.localInitialize(hostname, port, username, password);
        ServerUtils.registerPlugin("/Users/hieunguyen/work/code/java/OIM/ABB/lib.zip");
        SchedulerServiceUtils.executeJob("SmartForm User Recon");
    }

    @Test
    public void parse() throws JsonProcessingException {
        String jsonString = "[{Id=2, GuidId=4ed3b8ff-781c-46bd-b431-9bc9527be4f8, RoleName=Vai?tr??Nh?p?OnlineBanking}, {Id=7, GuidId=b21e4d42-bff0-454b-a6f5-718c16a0c5df, RoleName=Vai?tr??m?c???nh}, {Id=8, GuidId=bd1fe0a0-d04a-4eb6-85e2-c3dd8aaa8e49, RoleName=Vai?tr??giao?d?ch?vi?n}, {Id=11, GuidId=8625aa6b-2db7-4cf4-a59c-74df54186044, RoleName=Vai?tr??t?m?ki?m?th?ng?tin?kh?ch?h?ng}, {Id=12, GuidId=0557b23a-3c38-49eb-83d0-ff4bceaa67d7, RoleName=Vai?tr??truy?v?n?th?ng?tin?KH}, {Id=23, GuidId=vai-tro-gdv-sec, RoleName=Vai?tr??gdv?Sec}, {Id=24, GuidId=vai-tro-dieu-chuyen-sec, RoleName=Vai?tr??HO??i?u?chuy?n?Sec}, {Id=25, GuidId=vai-tro-dvkd-dieu-chuyen-sec, RoleName=Vai?tr??DVKD??i?u?chuy?n?Sec}]";
        jsonString = jsonString.replaceAll("^\\[\\{(.+)}]$", "$1");
        Set<Attribute> roles = new HashSet<>();
        Arrays.stream(jsonString.split("}\\s?,\\s?\\{")).forEach(x -> {
            AttributeBuilder ab = new AttributeBuilder();
            ab.setName("RoleId");
            String value = Arrays.stream(x.split(",\\s?")).filter(attributeValue -> attributeValue.toUpperCase().contains("ID")).collect(Collectors.toList()).get(0).replaceAll(".+=", "");
            ab.addValue(value);
            roles.add(ab.build());
        });
        System.out.println(roles);
    }

    @Test
    public void recon() throws Exception {
        OIMUtils.localInitialize(hostname, port, username, password);
        SmartFormUserReconciliation task = new SmartFormUserReconciliation();
        HashMap<String, Object> params = new HashMap<>();
        params.put("Application Name", "SmartForm");
        task.execute(params);
    }

}