export const defaultTableProps = {
    paginator: true,
    rows: 20,
    rowsPerPageOptions: [10, 20, 50, 100],
    emptyMessage: "Keine Daten gefunden.",
    paginatorTemplate: "FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown",
    currentPageReportTemplate: "Zeige {first} bis {last} von {totalRecords} Einträgen",
    stripedRows: true,
    responsiveLayout: "scroll" as const
};