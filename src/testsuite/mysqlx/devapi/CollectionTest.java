/*
  Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.

  The MySQL Connector/J is licensed under the terms of the GPLv2
  <http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>, like most MySQL Connectors.
  There are special exceptions to the terms and conditions of the GPLv2 as it is applied to
  this software, see the FLOSS License Exception
  <http://www.mysql.com/about/legal/licensing/foss-exception.html>.

  This program is free software; you can redistribute it and/or modify it under the terms
  of the GNU General Public License as published by the Free Software Foundation; version 2
  of the License.

  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along with this
  program; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth
  Floor, Boston, MA 02110-1301  USA

 */

package testsuite.mysqlx.devapi;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Test;

import com.mysql.cj.api.x.Collection;
import com.mysql.cj.api.x.DbDoc;
import com.mysql.cj.api.x.FetchedDocs;
import com.mysql.cj.api.x.Result;
import com.mysql.cj.core.exceptions.MysqlErrorNumbers;
import com.mysql.cj.core.exceptions.WrongArgumentException;
import com.mysql.cj.mysqlx.MysqlxError;
import com.mysql.cj.x.json.JsonDoc;
import com.mysql.cj.x.json.JsonValueString;

public class CollectionTest extends BaseDevApiTest {
    /** Collection for testing. */
    private Collection collection;

    public CollectionTest() throws Exception {
        try {
            // it probably won't exist, but drop it just in case
            this.schema.getCollection("CollectionTest").drop();
        } catch (MysqlxError ex) {
            if (ex.getErrorCode() != MysqlErrorNumbers.ER_BAD_TABLE_ERROR) {
                throw ex;
            }
        }
        this.collection = this.schema.createCollection("CollectionTest");
    }

    @Test
    public void testBasicAddString() {
        String json = "{'firstName':'Frank', 'middleName':'Lloyd', 'lastName':'Wright'}".replaceAll("'", "\"");
        Result res = this.collection.add(json).execute();
        assertTrue(res.getLastDocumentId().matches("[a-f0-9]{32}"));

        // verify existence
        FetchedDocs docs = this.collection.find("@.firstName like '%Fra%'").execute();
        // TODO: JsonDoc/DbDoc equivalence? slippery slope, or approaching unification?
        DbDoc d = docs.next();
        JsonDoc jd = (JsonDoc) d;
        JsonValueString val = (JsonValueString) jd.get("lastName");
        assertEquals("Wright", val.getValue());
    }

    @Test
    public void testBasicAddDoc() {
        JsonDoc doc = new JsonDoc().add("firstName", new JsonValueString().setValue("Georgia"));
        doc.add("middleName", new JsonValueString().setValue("Totto"));
        doc.add("lastName", new JsonValueString().setValue("O'Keeffe"));
        Result res = this.collection.add(doc).execute();
        assertTrue(res.getLastDocumentId().matches("[a-f0-9]{32}"));

        // verify existence
        FetchedDocs docs = this.collection.find("@.lastName like 'O\\'Kee%'").execute();
        // TODO: JsonDoc/DbDoc equivalence? slippery slope, or approaching unification?
        DbDoc d = docs.next();
        JsonDoc jd = (JsonDoc) d;
        JsonValueString val = (JsonValueString) jd.get("lastName");
        assertEquals("O'Keeffe", val.getValue());
    }

    @Test
    @Ignore("needs implemented")
    public void testBasicAddMap() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("x", 1);
        doc.put("y", "this is y");
        doc.put("z", new BigDecimal("44.22"));
        Result res = this.collection.add(doc).execute();
        assertTrue(res.getLastDocumentId().matches("[a-f0-9]{32}"));

        // verify existence
        FetchedDocs docs = this.collection.find("@.z >= 44.22").execute();
        // TODO: JsonDoc/DbDoc equivalence? slippery slope, or approaching unification?
        DbDoc d = docs.next();
        JsonDoc jd = (JsonDoc) d;
        JsonValueString val = (JsonValueString) jd.get("y");
        assertEquals("this is y", val.getValue());
    }

    @Test
    public void testAssignedId() {
        String json = "{'_id': 'Id#1', 'name': '<unknown>'}".replaceAll("'", "\"");
        Result res = this.collection.add(json).execute();
        assertNull(res.getLastDocumentId());

        // verify existence
        FetchedDocs docs = this.collection.find("@._id == 'Id#1'").execute();
        // TODO: JsonDoc/DbDoc equivalence? slippery slope, or approaching unification?
        DbDoc d = docs.next();
        JsonDoc jd = (JsonDoc) d;
        JsonValueString val = (JsonValueString) jd.get("name");
        assertEquals("<unknown>", val.getValue());
    }
}
