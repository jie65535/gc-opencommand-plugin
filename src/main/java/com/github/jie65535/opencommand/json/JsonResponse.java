/*
 * gc-opencommand
 * Copyright (C) 2022-2023 jie65535
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.github.jie65535.opencommand.json;

public final class JsonResponse {
    public int retcode = 200;
    public String message = "Success";
    public Object data;

    public JsonResponse() {

    }

    public JsonResponse(int code, String message) {
        this.retcode = code;
        this.message = message;
    }

    public JsonResponse(int code, String message, Object data) {
        this.retcode = code;
        this.message = message;
        this.data = data;
    }

    public JsonResponse(Object data) {
        this.data = data;
    }
}
