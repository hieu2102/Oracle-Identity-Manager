package vn.bnh.oim.scheduledtasks.lookups;

import Thor.API.Exceptions.*;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.scheduler.vo.TaskSupport;
import vn.bnh.oim.utils.LookupUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ReconcileLookups extends TaskSupport {
    private final ODLLogger logger = ODLLogger.getODLLogger(ReconcileLookups.class.getName());
    private HashMap<String, Object> params;
    private String lookupTablePrefix;
    private String fileDir;
    private String delimiter;

    @Override
    public void execute(HashMap hashMap) throws Exception {
        this.params = hashMap;
        this.setAttributes();
        Map<String, List<String>> lookupTables = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(this.fileDir))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(this.delimiter);
                if (values.length == 2) {
                    String title = values[0];
                    String license = values[1];
                    String lookupTable = lookupTablePrefix + "." + license;
                    if (lookupTables.containsKey(lookupTable)) {
                        lookupTables.get(lookupTable).add(title);
                    } else {
                        List<String> titles = new ArrayList<>();
                        titles.add(title);
                        lookupTables.put(lookupTable, titles);
                    }
                }
            }
            lookupTables.forEach((lookupTable, keyList) -> {
                try {
                    System.out.printf("[%s] Create Lookup Table %s", this.getClass().getCanonicalName(), lookupTable);
                    LookupUtils.createLookupTable(lookupTable);
                    System.out.printf("[%s] add lookup entries", this.getClass().getCanonicalName());
                    HashMap<String, String> lookupEntries = new HashMap<>();
                    for (String s : keyList) {
                        lookupEntries.put(s, s);
                    }
                    LookupUtils.updateLookupTable(lookupTable, lookupEntries);
                } catch (tcAPIException | tcDuplicateLookupCodeException | tcInvalidValueException |
                         tcColumnNotFoundException | tcInvalidLookupException e) {
                    throw new RuntimeException(e);
                }
            });
        }

    }

    @Override
    public HashMap getAttributes() {
        return null;
    }

    @Override
    public void setAttributes() {
        this.lookupTablePrefix = params.get("Lookup Prefix").toString();
        this.fileDir = params.get("File Dir").toString();
        this.delimiter = params.get("Value Delimiter").toString();
    }
}
