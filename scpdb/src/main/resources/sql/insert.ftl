INSERT INTO patientinfo(
    patid,
    patname,
    accnum,
    birthdate,
    birthtime,
    patsex,
    patage
)VALUES (
    <@p name="p.patid"/>,
    <@p name="p.patname"/>,
    <@p name="p.accnum"/>,
    <@p name="p.birthdate"/>,
    <@p name="p.birthtime"/>,
    <@p name="p.patsex"/>,
    <@p name="p.patage"/>
);