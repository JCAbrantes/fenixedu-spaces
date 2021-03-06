package org.fenixedu.spaces.services;

import static org.fenixedu.bennu.FenixEduSpaceConfiguration.BUNDLE;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.fenixedu.spaces.domain.Space;
import org.fenixedu.spaces.domain.SpaceClassification;
import org.fenixedu.spaces.domain.occupation.SharedOccupation;
import org.fenixedu.spaces.ui.InformationBean;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ExportSpace {

    private static List<Object> getHeaders(List<String> metaKeys) {
        final List<Object> headers = new ArrayList<Object>();
        headers.add(BundleUtil.getString(BUNDLE, "export.excel.path"));
        headers.add(BundleUtil.getString(BUNDLE, "export.excel.name"));
        headers.add(BundleUtil.getString(BUNDLE, "export.excel.id"));
        headers.add(BundleUtil.getString(BUNDLE, "export.excel.valid.from"));
        headers.add(BundleUtil.getString(BUNDLE, "export.excel.valid.until"));
        headers.add(BundleUtil.getString(BUNDLE, "export.excel.blueprintNumber"));
        headers.add(BundleUtil.getString(BUNDLE, "export.excel.classification"));
        headers.add(BundleUtil.getString(BUNDLE, "export.excel.capacity"));
        headers.add(BundleUtil.getString(BUNDLE, "export.excel.area"));
        headers.add(BundleUtil.getString(BUNDLE, "export.excel.occupants"));
        HashMap<String, LocalizedString> showKeys = new HashMap<String, LocalizedString>();
        for (SpaceClassification spaceClassification : SpaceClassification.all()) {
            for (JsonElement je : spaceClassification.getMetadataSpec().getAsJsonArray()) {
                JsonObject attribute = je.getAsJsonObject();
                String name = attribute.get("name").getAsString();
                LocalizedString description = LocalizedString.fromJson(attribute.get("description"));
                if (showKeys.putIfAbsent(name, description) == null) {
                    metaKeys.add(name);
                }
            }
        }

        for (String header : metaKeys) {
            headers.add(showKeys.get(header).getContent());
        }
        return headers;
    }

    private static void exportToXls(Space space, OutputStream outputStream) throws IOException {
        List<String> metaKeys = new ArrayList<String>();
        final List<Object> headers = getHeaders(metaKeys);
        final Spreadsheet spreadsheet = new Spreadsheet("GestãoDeEspaços", headers);
        fillSpaceInfo(space, spreadsheet, metaKeys);
        fillSpreadSheet(space, spreadsheet, metaKeys);
        spreadsheet.exportToXLSSheet(outputStream);
    }

    private static String StringPath(List<Space> path) {
        return path.stream().map(a -> a.getName()).collect(Collectors.joining(" > "));
    }

    private static void fillSpaceInfo(Space space, final Spreadsheet spreadsheet, List<String> metaKeys) {
        final Row row = spreadsheet.addRow();

        row.setCell((space.getParent() != null) ? StringPath(space.getParent().getPath()) : "--");
        row.setCell(space.getName());
        row.setCell(space.bean().getIdentification() != null ? space.bean().getIdentification() : "--");
        row.setCell(
                space.bean().getValidFrom() != null ? space.bean().getValidFrom().toString(InformationBean.DATE_FORMAT) : "--");
        row.setCell(
                space.bean().getValidUntil() != null ? space.bean().getValidUntil().toString(InformationBean.DATE_FORMAT) : "--");
        row.setCell(space.bean().getBlueprintNumber() != null ? space.bean().getBlueprintNumber() : "--");
        row.setCell(space.bean().getClassification() != null ? space.bean().getClassification().getName().getContent() : "--");
        row.setCell(space.bean().getAllocatableCapacity() != null ? space.bean().getAllocatableCapacity().toString() : "--");
        row.setCell(space.bean().getArea() != null ? space.bean().getArea().toString() : "--");
        row.setCell(getOccupantsString(space));
        for (String field : metaKeys) {
            row.setCell((space.getMetadata(field).orElse("--")).toString());
        }
    }

    private static String getOccupantsString(Space space) {
        return space.getOccupationSet().stream().filter(occ -> SharedOccupation.class.isInstance(occ))
                .map(occ -> (SharedOccupation) occ).filter(so -> so.isActive()).map(so -> {
                    return so.getUser().getProfile().getDisplayName() + " (" + so.getUser().getUsername() + ")";
                }).collect(Collectors.joining("; "));
    }

    private static void fillSpreadSheet(Space space, final Spreadsheet spreadsheet, List<String> metaKeys) {
        for (Space subSpace : space.getChildren()) {
            if (subSpace.isActive()) {
                fillSpaceInfo(subSpace, spreadsheet, metaKeys);
            }
            if (subSpace.getChildren().size() != 0) {
                fillSpreadSheet(subSpace, spreadsheet, metaKeys);
            }
        }
    }

    public static void run(Space space, OutputStream outputStream) {
        try {
            exportToXls(space, outputStream);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
