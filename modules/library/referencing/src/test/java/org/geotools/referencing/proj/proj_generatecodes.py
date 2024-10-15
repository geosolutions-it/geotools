import csv
from pyproj import CRS

output_file = "epsg_proj_definitions.csv"
with open(output_file, mode="w", newline='') as csv_file:
    writer = csv.writer(csv_file)
    writer.writerow(["EPSG Code", "PROJ Definition"])
    
    # Iterate over EPSG codes supported by pyproj
    for epsg_code in range(1, 1000000):
        try:
            crs = CRS.from_epsg(epsg_code)
            proj_string = crs.to_proj4()
            writer.writerow([epsg_code, proj_string])
        except Exception as e:
            continue

print(f"CSV file '{output_file}' generated successfully.")
