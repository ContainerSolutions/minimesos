package com.containersol.minimesos;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.containersol.minimesos.state.Framework;
import com.containersol.minimesos.state.State;
import org.junit.Test;

import static org.junit.Assert.*;

public class ParseStateJSONTest {

    public final String EXAMPLE_STATE_JSON = "{" +
        "\"activated_slaves\": 3," +
        "\"build_date\": \"2015-05-05 06:15:50\"," +
        "\"build_time\": 1430806550," +
        "\"build_user\": \"root\"," +
        "\"completed_frameworks\": [" +
            "" +
        "]," +
        "\"deactivated_slaves\": 0," +
        "\"elected_time\": 1441628978.271," +
        "\"failed_tasks\": 0," +
        "\"finished_tasks\": 0," +
        "\"flags\": {" +
            "\"allocation_interval\": \"1secs\"," +
            "\"authenticate\": \"false\"," +
            "\"authenticate_slaves\": \"false\"," +
            "\"authenticators\": \"crammd5\"," +
            "\"framework_sorter\": \"drf\"," +
            "\"help\": \"false\"," +
            "\"initialize_driver_logging\": \"true\"," +
            "\"ip\": \"0.0.0.0\"," +
            "\"log_auto_initialize\": \"true\"," +
            "\"log_dir\": \"\\/var\\/log\"," +
            "\"logbufsecs\": \"0\"," +
            "\"logging_level\": \"INFO\"," +
            "\"port\": \"5050\"," +
            "\"quiet\": \"false\"," +
            "\"quorum\": \"1\"," +
            "\"recovery_slave_removal_limit\": \"100%\"," +
            "\"registry\": \"replicated_log\"," +
            "\"registry_fetch_timeout\": \"1mins\"," +
            "\"registry_store_timeout\": \"5secs\"," +
            "\"registry_strict\": \"false\"," +
            "\"root_submissions\": \"true\"," +
            "\"slave_reregister_timeout\": \"10mins\"," +
            "\"user_sorter\": \"drf\"," +
            "\"version\": \"false\"," +
            "\"webui_dir\": \"\\/usr\\/share\\/mesos\\/webui\"," +
            "\"work_dir\": \"\\/var\\/lib\\/mesos\"," +
            "\"zk\": \"zk:\\/\\/localhost:2181\\/mesos\"," +
            "\"zk_session_timeout\": \"10secs\"" +
        "}," +
        "\"frameworks\": [" +
            "{" +
                "\"active\": true," +
                "\"checkpoint\": true," +
                "\"completed_tasks\": [" +
                    "" +
                "]," +
                "\"failover_timeout\": 2592000," +
                "\"hostname\": \"0f43d2f7606a\"," +
                "\"id\": \"20150907-122934-3858764204-5050-23-0000\"," +
                "\"name\": \"elasticsearch\"," +
                "\"offered_resources\": {" +
                    "\"cpus\": 0," +
                    "\"disk\": 0," +
                    "\"mem\": 0" +
                "}," +
                "\"offers\": [" +
                    "" +
                "]," +
                "\"registered_time\": 1441629007.9145," +
                "\"resources\": {" +
                    "\"cpus\": 3," +
                    "\"disk\": 3072," +
                    "\"mem\": 768," +
                    "\"ports\": \"[9200-9202, 9300-9302]\"" +
                "}," +
                "\"role\": \"*\"," +
                "\"tasks\": [" +
                    "{" +
                        "\"discovery\": {" +
                            "\"ports\": {" +
                                "\"ports\": [" +
                                    "{" +
                                        "\"name\": \"CLIENT_PORT\"," +
                                        "\"number\": 9200" +
                                    "}," +
                                    "{" +
                                        "\"name\": \"TRANSPORT_PORT\"," +
                                        "\"number\": 9300" +
                                    "}" +
                                "]" +
                            "}," +
                            "\"visibility\": \"EXTERNAL\"" +
                        "}," +
                        "\"executor_id\": \"29deeca9-0f28-4df7-af1d-14ae790044f6\"," +
                        "\"framework_id\": \"20150907-122934-3858764204-5050-23-0000\"," +
                        "\"id\": \"elasticsearch_slave1_20150907T123008.379Z\"," +
                        "\"labels\": [" +
                            "" +
                        "]," +
                        "\"name\": \"esdemo\"," +
                        "\"resources\": {" +
                            "\"cpus\": 1," +
                            "\"disk\": 1024," +
                            "\"mem\": 256," +
                            "\"ports\": \"[9200-9200, 9300-9300]\"" +
                        "}," +
                        "\"slave_id\": \"20150907-122934-3858764204-5050-23-S1\"," +
                        "\"state\": \"TASK_RUNNING\"," +
                        "\"statuses\": [" +
                            "{" +
                                "\"state\": \"TASK_STARTING\"," +
                                "\"timestamp\": 1441629015.6595" +
                            "}," +
                            "{" +
                                "\"state\": \"TASK_RUNNING\"," +
                                "\"timestamp\": 1441629278.4553" +
                            "}" +
                        "]" +
                    "}," +
                    "{" +
                        "\"discovery\": {" +
                            "\"ports\": {" +
                                "\"ports\": [" +
                                    "{" +
                                        "\"name\": \"CLIENT_PORT\"," +
                                        "\"number\": 9202" +
                                    "}," +
                                    "{" +
                                        "\"name\": \"TRANSPORT_PORT\"," +
                                        "\"number\": 9302" +
                                    "}" +
                                "]" +
                            "}," +
                            "\"visibility\": \"EXTERNAL\"" +
                        "}," +
                        "\"executor_id\": \"ec8dee06-4176-4d7e-b5e1-9e6f1f8b5fdf\"," +
                        "\"framework_id\": \"20150907-122934-3858764204-5050-23-0000\"," +
                        "\"id\": \"elasticsearch_slave3_20150907T123008.294Z\"," +
                        "\"labels\": [" +
                            "" +
                        "]," +
                        "\"name\": \"esdemo\"," +
                        "\"resources\": {" +
                            "\"cpus\": 1," +
                            "\"disk\": 1024," +
                            "\"mem\": 256," +
                            "\"ports\": \"[9202-9202, 9302-9302]\"" +
                        "}," +
                        "\"slave_id\": \"20150907-122934-3858764204-5050-23-S0\"," +
                        "\"state\": \"TASK_RUNNING\"," +
                        "\"statuses\": [" +
                            "{" +
                                "\"state\": \"TASK_STARTING\"," +
                                "\"timestamp\": 1441629015.3181" +
                            "}," +
                            "{" +
                                "\"state\": \"TASK_RUNNING\"," +
                                "\"timestamp\": 1441629278.3756" +
                            "}" +
                        "]" +
                    "}," +
                    "{" +
                        "\"discovery\": {" +
                            "\"ports\": {" +
                                "\"ports\": [" +
                                    "{" +
                                        "\"name\": \"CLIENT_PORT\"," +
                                        "\"number\": 9201" +
                                    "}," +
                                    "{" +
                                        "\"name\": \"TRANSPORT_PORT\"," +
                                        "\"number\": 9301" +
                                    "}" +
                                "]" +
                            "}," +
                            "\"visibility\": \"EXTERNAL\"" +
                        "}," +
                        "\"executor_id\": \"5aa2fc1d-6ad5-4710-9f47-1f9ddcf01ecb\"," +
                        "\"framework_id\": \"20150907-122934-3858764204-5050-23-0000\"," +
                        "\"id\": \"elasticsearch_slave2_20150907T123008.041Z\"," +
                        "\"labels\": [" +
                            "" +
                        "]," +
                        "\"name\": \"esdemo\"," +
                        "\"resources\": {" +
                            "\"cpus\": 1," +
                            "\"disk\": 1024," +
                            "\"mem\": 256," +
                            "\"ports\": \"[9201-9201, 9301-9301]\"" +
                        "}," +
                        "\"slave_id\": \"20150907-122934-3858764204-5050-23-S2\"," +
                        "\"state\": \"TASK_RUNNING\"," +
                        "\"statuses\": [" +
                            "{" +
                                "\"state\": \"TASK_STARTING\"," +
                                "\"timestamp\": 1441629015.8581" +
                            "}," +
                            "{" +
                                "\"state\": \"TASK_RUNNING\"," +
                                "\"timestamp\": 1441629278.2919" +
                            "}" +
                        "]" +
                    "}" +
                "]," +
                "\"unregistered_time\": 0," +
                "\"used_resources\": {" +
                    "\"cpus\": 3," +
                    "\"disk\": 3072," +
                    "\"mem\": 768," +
                    "\"ports\": \"[9200-9202, 9300-9302]\"" +
                "}," +
                "\"user\": \"root\"," +
                "\"webui_url\": \"http:\\/\\/0f43d2f7606a:31100\"" +
            "}" +
        "]," +
        "\"git_sha\": \"d6309f92a7f9af3ab61a878403e3d9c284ea87e0\"," +
        "\"git_tag\": \"0.22.1\"," +
        "\"hostname\": \"d3666e54bf39\"," +
        "\"id\": \"20150907-122934-3858764204-5050-23\"," +
        "\"killed_tasks\": 0," +
        "\"leader\": \"master@172.17.0.230:5050\"," +
        "\"log_dir\": \"\\/var\\/log\"," +
        "\"lost_tasks\": 0," +
        "\"orphan_tasks\": [" +
            "" +
        "]," +
        "\"pid\": \"master@172.17.0.230:5050\"," +
        "\"slaves\": [" +
            "{" +
                "\"active\": true," +
                "\"attributes\": {" +
                    "" +
                "}," +
                "\"hostname\": \"slave2\"," +
                "\"id\": \"20150907-122934-3858764204-5050-23-S2\"," +
                "\"pid\": \"slave(1)@172.17.0.230:5052\"," +
                "\"registered_time\": 1441628979.0617," +
                "\"resources\": {" +
                    "\"cpus\": 3," +
                    "\"disk\": 13483," +
                    "\"mem\": 4936," +
                    "\"ports\": \"[9201-9201, 9301-9301]\"" +
                "}" +
            "}," +
            "{" +
                "\"active\": true," +
                "\"attributes\": {" +
                    "" +
                "}," +
                "\"hostname\": \"slave1\"," +
                "\"id\": \"20150907-122934-3858764204-5050-23-S1\"," +
                "\"pid\": \"slave(1)@172.17.0.230:5051\"," +
                "\"registered_time\": 1441628978.5892," +
                "\"resources\": {" +
                    "\"cpus\": 3," +
                    "\"disk\": 13483," +
                    "\"mem\": 4936," +
                    "\"ports\": \"[9200-9200, 9300-9300]\"" +
                "}" +
            "}," +
            "{" +
                "\"active\": true," +
                "\"attributes\": {" +
                    "" +
                "}," +
                "\"hostname\": \"slave3\"," +
                "\"id\": \"20150907-122934-3858764204-5050-23-S0\"," +
                "\"pid\": \"slave(1)@172.17.0.230:5053\"," +
                "\"registered_time\": 1441628978.5096," +
                "\"resources\": {" +
                    "\"cpus\": 3," +
                    "\"disk\": 13483," +
                    "\"mem\": 4936," +
                    "\"ports\": \"[9202-9202, 9302-9302]\"" +
                "}" +
            "}" +
        "]," +
        "\"staged_tasks\": 3," +
        "\"start_time\": 1441628974.9045," +
        "\"started_tasks\": 3," +
        "\"unregistered_frameworks\": [" +
            "" +
        "]," +
        "\"version\": \"0.22.1\"" +
    "}";

    @Test
    public void exampleStateJSONIsParsedCorrectly() throws JsonParseException, JsonMappingException {
        State parsedState = State.fromJSON(EXAMPLE_STATE_JSON);
        assertEquals(1, parsedState.getFrameworks().size());
        Framework framework = parsedState.getFramework("elasticsearch");
        assertNotNull(framework);
        assertEquals("elasticsearch", framework.getName());
        assertEquals(true, framework.isActive());
        assertEquals(true, framework.isCheckpoint());
        assertEquals(2592000, framework.getFailoverTimeout());
        assertEquals("0f43d2f7606a", framework.getHostname());
        assertEquals("20150907-122934-3858764204-5050-23-0000", framework.getId());
        assertEquals("elasticsearch", framework.getName());
        assertEquals("*", framework.getRole());
    }
}
