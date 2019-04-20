/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.zookeeper.cli;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.commons.cli.PosixParser;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

/**
 * ll command for cli
 */
public class LlCommand extends CliCommand {

    private static Options options = new Options();
    private String args[];

    public LlCommand() {
        super("ll", "path");
    }

    @Override
    public CliCommand parse(String[] cmdArgs) throws CliParseException {
        Parser parser = new PosixParser();
        CommandLine cl;
        try {
            cl = parser.parse(options, cmdArgs);
        } catch (ParseException ex) {
            throw new CliParseException(ex);
        }
        args = cl.getArgs();
        if (args.length < 2) {
            throw new CliParseException(getUsageStr());
        }

        return this;
    }

    @Override
    public boolean exec() throws CliException {
        String parentPath = args[1];
        try {
            List<String> children = zk.getChildren(parentPath, null, null);

            int totalDataLength = 0;
            Collections.sort(children);
            StringBuilder sb = new StringBuilder();
            Stat stat;
            for (String child : children) {
                stat = new Stat();
                String childPath = "/".equals(parentPath) ? parentPath + child : parentPath+ "/" + child;
                List<ACL> acl = zk.getACL(childPath, stat);
                sb.append(printAclString(acl));
                sb.append("\t");
                totalDataLength += stat.getDataLength();
                sb.append(stat.getDataLength());
                sb.append("\t");
                sb.append(stat.getNumChildren());
                sb.append("\t");
                sb.append(transferLongToDate(stat.getMzxid()));
                sb.append("\t");
                sb.append(child);
                sb.append("\n");
            }
            sb.insert(0, "total "+ totalDataLength + "\n");

            out.println(sb.toString());
        } catch (IllegalArgumentException ex) {
            throw new MalformedPathException(ex.getMessage());
        } catch (KeeperException | InterruptedException ex) {
            throw new CliWrapperException(ex);
        }

        return false;
    }

    private String printAclString(List<ACL> acl) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < acl.size(); i++) {
            ACL a = acl.get(i);
            if (i == 0) {
                sb.append(GetAclCommand.getPermString(a.getPerms()) + ": " + a.getId());
            } else {
                sb.append(";" + GetAclCommand.getPermString(a.getPerms()) + ": " + a.getId());
            }
        }
        return sb.toString();
    }

    private String transferLongToDate(long millSec) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(millSec);
        return sdf.format(date);
    }
}
