/*
  Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.

  The MySQL Connector/J is licensed under the terms of the GPLv2
  <http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>, like most MySQL Connectors.
  There are special exceptions to the terms and conditions of the GPLv2 as it is applied to
  this software, see the FOSS License Exception
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

package com.mysql.cj.xdevapi;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.mysql.cj.api.xdevapi.JsonValue;
import com.mysql.cj.core.Messages;
import com.mysql.cj.core.exceptions.AssertionFailedException;
import com.mysql.cj.x.core.XDevAPIError;

public class CreateIndexParams {
    private String indexName;
    /** One of INDEX or SPATIAL. Default is INDEX and may be omitted. **/
    private String indexType = "INDEX";
    private List<IndexField> fields = new ArrayList<>();

    public CreateIndexParams(String indexName, DbDoc indexDefinition) {
        init(indexName, indexDefinition);
    }

    public CreateIndexParams(String indexName, String jsonIndexDefinition) {
        if (jsonIndexDefinition == null || jsonIndexDefinition.trim().length() == 0) {
            throw new XDevAPIError(Messages.getString("CreateIndexParams.0", new String[] { "jsonIndexDefinition" }));
        }
        try {
            init(indexName, JsonParser.parseDoc(new StringReader(jsonIndexDefinition)));
        } catch (IOException ex) {
            throw AssertionFailedException.shouldNotHappen(ex);
        }
    }

    private void init(String idxName, DbDoc indexDefinition) {
        if (idxName == null || idxName.trim().length() == 0) {
            throw new XDevAPIError(Messages.getString("CreateIndexParams.0", new String[] { "indexName" }));
        }
        if (indexDefinition == null) {
            throw new XDevAPIError(Messages.getString("CreateIndexParams.0", new String[] { "indexDefinition" }));
        }

        this.indexName = idxName;

        JsonValue val = indexDefinition.get("type");
        if (val != null) {
            if (val instanceof JsonString) {
                String type = ((JsonString) val).getString();
                if ("INDEX".equalsIgnoreCase(type) || "SPATIAL".equalsIgnoreCase(type)) {
                    this.indexType = type;
                } else {
                    throw new XDevAPIError("Wrong index type '" + type + "'. Must be 'INDEX' or 'SPATIAL'.");
                }
            } else {
                throw new XDevAPIError("Index type must be a string.");
            }
        }

        val = indexDefinition.get("fields");
        if (val != null) {
            if (val instanceof JsonArray) {
                for (JsonValue field : (JsonArray) val) {
                    if (field instanceof DbDoc) {
                        this.fields.add(new IndexField((DbDoc) field));
                    } else {
                        throw new XDevAPIError("Index field definition must be a JSON document.");
                    }
                }

            } else {
                throw new XDevAPIError("Index definition 'fields' member must be an array of index fields.");
            }
        } else {
            throw new XDevAPIError("Index definition does not contain fields.");
        }
    }

    public String getIndexName() {
        return this.indexName;
    }

    public String getIndexType() {
        return this.indexType;
    }

    public List<IndexField> getFields() {
        return this.fields;
    }

    public static class IndexField {

        /** The full document path to the document member or field to be indexed **/
        private String field;

        /**
         * One of the supported SQL column types to map the field into.
         * For numeric types, the optional UNSIGNED keyword may follow. For
         * the TEXT type, the length to consider for indexing may be added.
         **/
        private String type;

        /** (optional) true if the field is required to exist in the document. defaults to false, except for GEOJSON where it defaults to true **/
        private boolean required = false;

        /** (optional) special option flags for use when decoding GEOJSON data **/
        private Integer options = null;

        /** (optional) srid value for use when decoding GEOJSON data **/
        private Integer srid = null;

        public IndexField(DbDoc indexField) {
            JsonValue val = indexField.get("field");
            if (val != null) {
                if (val instanceof JsonString) {
                    this.field = ((JsonString) val).getString();
                } else {
                    throw new XDevAPIError("Index field 'field' member must be a string.");
                }
            } else {
                throw new XDevAPIError("Index field definition has no document path.");
            }

            val = indexField.get("type");
            if (val != null) {
                if (val instanceof JsonString) {
                    this.type = ((JsonString) val).getString();
                    // TODO pure "TEXT" is not allowed as a type, server requires the length specification
                    // we're waiting for clarification about whether we set some default on client side in that case, eg.:
                    //    if ("TEXT".equals(this.type)) {this.type = "TEXT(64)";}
                    // or we do nothing and user has to specify TEXT(n) always
                } else {
                    throw new XDevAPIError("Index type must be a string.");
                }
            } else {
                throw new XDevAPIError("Index field definition has no field type.");
            }

            val = indexField.get("required");
            if (val != null) {
                if (val instanceof JsonLiteral && !JsonLiteral.NULL.equals(val)) {
                    this.required = Boolean.valueOf(((JsonLiteral) val).value);
                } else {
                    throw new XDevAPIError("Index field 'required' member must be boolean.");
                }
            } else if (this.type.equalsIgnoreCase("GEOJSON")) {
                this.required = true;
            }

            val = indexField.get("options");
            if (val != null) {
                if (this.type.equalsIgnoreCase("GEOJSON")) {
                    if (val instanceof JsonNumber) {
                        this.options = ((JsonNumber) val).getInteger();
                    } else {
                        throw new XDevAPIError("Index field 'options' member must be integer.");
                    }
                } else {
                    throw new XDevAPIError("Index field 'options' member should not be used for field types other than GEOJSON.");
                }
            }

            val = indexField.get("srid");
            if (val != null) {
                if (this.type.equalsIgnoreCase("GEOJSON")) {
                    if (val instanceof JsonNumber) {
                        this.srid = ((JsonNumber) val).getInteger();
                    } else {
                        throw new XDevAPIError("Index field 'srid' member must be integer.");
                    }
                } else {
                    throw new XDevAPIError("Index field 'srid' member should not be used for field types other than GEOJSON.");
                }
            }
        }

        public String getField() {
            return this.field;
        }

        public String getType() {
            return this.type;
        }

        public boolean isRequired() {
            return this.required;
        }

        public Integer getOptions() {
            return this.options;
        }

        public Integer getSrid() {
            return this.srid;
        }
    }
}
