import { NuIcon } from "../../common";
import React, { useMemo } from "react";
import { Stack } from "@mui/material";
import { useTranslation } from "react-i18next";

export function FilterListItemLabel({ name, icon }: { name: string; icon?: string }): JSX.Element {
    const { t } = useTranslation();

    const label = useMemo(() => {
        switch (name) {
            case "createdAt":
                return t("table.filter.sortBy.createdAt", "Creation date");
            case "modificationDate":
                return t("table.filter.sortBy.modificationDate", "Modification date");
            case "name":
                return t("table.filter.sortBy.name", "Name");
            default:
                return name;
        }
    }, [name, t]);

    return icon ? (
        <Stack direction="row" spacing={1} alignItems="center">
            <span>{label}</span>
            <NuIcon
                src={icon}
                sx={{ color: "primary.main" }}
                style={{
                    fontSize: "1.2em",
                }}
            />
        </Stack>
    ) : (
        <>{label}</>
    );
}
